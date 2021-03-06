package org.openstack.atlas.api.resources;

import org.openstack.atlas.docs.loadbalancers.api.v1.ConnectionLogging;
import org.openstack.atlas.service.domain.entities.LoadBalancer;
import org.openstack.atlas.service.domain.operations.Operation;
import org.openstack.atlas.api.helpers.ResponseFactory;
import org.openstack.atlas.api.repository.ValidatorRepository;
import org.openstack.atlas.api.resources.providers.CommonDependencyProvider;
import org.openstack.atlas.api.validation.context.HttpRequestType;
import org.openstack.atlas.api.validation.results.ValidatorResult;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

public class ConnectionLoggingResource extends CommonDependencyProvider {

    private Integer accountId;
    private Integer loadBalancerId;
    private HttpHeaders requestHeaders;

    @GET
    public Response retrieveConnectionLogging() {
        Boolean isEnabled;
        try {
            isEnabled = connectionLoggingService.get(accountId, loadBalancerId);
            ConnectionLogging connectionLogging = new ConnectionLogging();
            connectionLogging.setEnabled(isEnabled);
            return Response.status(200).entity(connectionLogging).build();
        } catch (Exception e) {
            return ResponseFactory.getErrorResponse(e, null, null);
        }
    }

    @PUT
    @Consumes({APPLICATION_XML, APPLICATION_JSON})
    public Response updateConnectionLogging(ConnectionLogging conLog) {
        ValidatorResult result = ValidatorRepository.getValidatorFor(ConnectionLogging.class).validate(conLog, HttpRequestType.PUT);
        if (!result.passedValidation()) {
            return getValidationFaultResponse(result);
        }

        try {
            org.openstack.atlas.docs.loadbalancers.api.v1.LoadBalancer apiLb = new org.openstack.atlas.docs.loadbalancers.api.v1.LoadBalancer();
            apiLb.setConnectionLogging(conLog);
            LoadBalancer domainLb = dozerMapper.map(apiLb, LoadBalancer.class);
            domainLb.setId(loadBalancerId);
            domainLb.setAccountId(accountId);
            if (requestHeaders != null) {
                domainLb.setUserName(requestHeaders.getRequestHeader("X-PP-User").get(0));
            }
            connectionLoggingService.update(domainLb);
            asyncService.callAsyncLoadBalancingOperation(Operation.UPDATE_CONNECTION_LOGGING, domainLb);
            return Response.status(Response.Status.ACCEPTED).build();
        } catch (Exception e) {
            return ResponseFactory.getErrorResponse(e, null, null);
        }
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public Integer getLoadBalancerId() {
        return loadBalancerId;
    }

    public void setLoadBalancerId(Integer loadBalancerId) {
        this.loadBalancerId = loadBalancerId;
    }

    public void setRequestHeaders(HttpHeaders requestHeaders) {
        this.requestHeaders = requestHeaders;
    }
}
