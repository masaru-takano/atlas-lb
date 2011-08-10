package org.openstack.atlas.service.domain.entity;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;

@javax.persistence.Entity
@Table(name = "connection_throttle")
public class ConnectionThrottle extends Entity implements Serializable {
    private final static long serialVersionUID = 532512316L;

	@OneToOne
	@JoinColumn(name = "loadbalancer_id")
	private LoadBalancer loadbalancer;

	@Column(name = "max_request_rate", nullable = false)
	private Integer maxRequestRate;

	@Column(name = "rate_interval", nullable = false)
	private Integer rateInterval;

	public LoadBalancer getLoadBalancer() {
		return loadbalancer;
	}

	public void setLoadBalancer(LoadBalancer loadBalancer) {
		this.loadbalancer = loadBalancer;
	}

	public Integer getMaxRequestRate() {
		return maxRequestRate;
	}

	public void setMaxRequestRate(Integer maxRequestRate) {
		this.maxRequestRate = maxRequestRate;
	}

	public Integer getRateInterval() {
		return rateInterval;
	}

	public void setRateInterval(Integer rateInterval) {
		this.rateInterval = rateInterval;
	}
}
