package org.openstack.atlas.logs.lzofaker;

import com.hadoop.compression.lzo.LzopCodec;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.hibernate.Query;
import org.joda.time.DateTime;
import org.openstack.atlas.config.HadoopLogsConfigs;

import org.openstack.atlas.docs.loadbalancers.api.v1.LoadBalancer;
import org.openstack.atlas.logs.hibernatetoy.HibernateDbConf;
import org.openstack.atlas.logs.hibernatetoy.HuApp;
import org.openstack.atlas.util.staticutils.StaticDateTimeUtils;
import org.openstack.atlas.util.staticutils.StaticFileUtils;

public class LzoFakerMain {

    private static final int SB_MAX_SIZE = 256 * 1024 + 4096;
    private static final int BUFFSIZE = 512 * 1024;
    private static final long ORD_MILLIS_PER_HOUR = 10000000;
    private static final int REAL_MILLIS_PER_HOUR = 60 * 60 * 1000;

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.printf("Usage is <configFile> <hourkey> <nLines> [lbId...]\n");
            System.out.printf("\n");
            System.out.printf("Test if the configuration file can be loaded.");
            return;
        }

        String jsonConfFileName = StaticFileUtils.expandUser(args[0]);
        int hourKey = Integer.parseInt(args[1]);
        int nLines = Integer.parseInt(args[2]);
        String lzoFileName = hourKey + "-access_log.aggregated.lzo";
        String lzoPath = StaticFileUtils.joinPath(HadoopLogsConfigs.getFileSystemRootDir(), lzoFileName);
        boolean useCustomLbs = false;
        Set<Integer> customLbIds = new HashSet<Integer>();
        if (args.length > 3) {
            useCustomLbs = true;
            for (int i = 3; i < args.length; i++) {
                customLbIds.add(Integer.parseInt(args[i]));
            }
        }
        HuApp huApp = new HuApp();
        HibernateDbConf hConf = HibernateDbConf.newHibernateConf(jsonConfFileName);

        System.out.printf("Useing db config %s\n", hConf.toString());

        huApp.setDbMap(hConf);

        huApp.begin();
        List<LoadBalancerIdAndName> lbs = new ArrayList<LoadBalancerIdAndName>();

        for (LoadBalancerIdAndName lb : LzoFakerMain.getActiveLoadbalancerIdsAndNames(huApp)) {
            System.out.printf("%s ", lb.toString());
            if (useCustomLbs) {
                if (customLbIds.contains(lb.getLoadbalancerId())) {
                    lbs.add(lb);
                    System.out.printf("USEING\n");
                }
                System.out.printf("SKIPPING\n");
            } else {
                lbs.add(lb);
                System.out.printf("USEING\n");
            }
        }

        huApp.commit();

        int lbArrayLength = lbs.size();
        LoadBalancerIdAndName[] lbArray = new LoadBalancerIdAndName[lbArrayLength];
        Collections.sort(lbs);
        for (int i = 0; i < lbArrayLength; i++) {
            lbArray[i] = lbs.get(i);
        }
        lbs = null;
        System.out.printf("Found a total of %d active loadbalancers\n", lbArrayLength);
        DateTime dt = StaticDateTimeUtils.OrdinalMillisToDateTime(hourKey * ORD_MILLIS_PER_HOUR, false);
        double stepMillis = (double) REAL_MILLIS_PER_HOUR / (double) nLines;

        Configuration conf = new Configuration();
        conf.set("io.compression.codecs", "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,com.hadoop.compression.lzo.LzoCodec,com.hadoop.compression.lzo.LzopCodec,org.apache.hadoop.io.compress.BZip2Codec");
        conf.set("io.compression.codec.lzo.class", "com.hadoop.compression.lzo.LzoCodec");
        LzopCodec codec = new LzopCodec();
        codec.setConf(conf);

        OutputStream os = StaticFileUtils.openOutputFile(lzoPath, BUFFSIZE);
        CompressionOutputStream cos = codec.createOutputStream(os);


        StringBuilder logBuilder = new StringBuilder(SB_MAX_SIZE);
        for (int i = 0; i < nLines; i++) {
            if (i % 100000 == 0) {
                System.out.printf("wrote %d lines %d lines left to go\n", i, nLines - i);
            }
            DateTime offsetDt = dt.plusMillis((int) (stepMillis * i));
            String apacheTime = StaticDateTimeUtils.toApacheDateTime(offsetDt);
            LoadBalancerIdAndName lb = lbArray[i % lbArrayLength];
            String vsName = lb.getAccountId() + "_" + lb.getLoadbalancerId();
            buildFakeLogLine(vsName, apacheTime, logBuilder);

            if (logBuilder.length() >= SB_MAX_SIZE) {
                cos.write(logBuilder.toString().getBytes());
                logBuilder.setLength(0);
            }
        }
        cos.write(logBuilder.toString().getBytes());
        cos.flush();
        cos.finish();

        StaticFileUtils.close(cos);
    }

    public static void buildFakeLogLine(String lbName, String apacheTime, StringBuilder sb) {
        sb.append(lbName).
                append(" www.refferer.com 127.0.0.1 - - [").append(apacheTime).
                append("] \"GET /blah/blah?f=u HTTP/1.1\" 200 2769 \"http://www.blah.org/pfft\" \"Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.57 Safari/537.17\" 127.0.0.1:80\n");
    }

    public static List<LoadBalancerIdAndName> getActiveLoadbalancerIdsAndNames(HuApp huApp) {
        List<LoadBalancerIdAndName> lbs = new ArrayList<LoadBalancerIdAndName>();
        String queryString = "select l.id,l.accountId,l.name from LoadBalancer l where l.status = 'ACTIVE'";

        // In the real repository just use q = entityManager.createQuery(queryString)
        Query q = huApp.getSession().createQuery(queryString);

        // In the real repository use List<Object> objList = q.getResultList()
        List<Object> rows = q.list();
        for (Object uncastedRowArrayObj : rows) {
            // Each element of rows is actually an Object[] whith each element representing a column
            Object[] row = (Object[]) uncastedRowArrayObj;
            LoadBalancerIdAndName lb = new LoadBalancerIdAndName();
            lb.setLoadbalancerId((Integer) row[0]);
            lb.setAccountId((Integer) row[1]);
            lb.setName((String) row[2]);
            lbs.add(lb);
        }

        return lbs;
    }
}