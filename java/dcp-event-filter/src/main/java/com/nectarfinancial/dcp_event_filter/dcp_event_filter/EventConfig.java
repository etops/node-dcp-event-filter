package com.nectarfinancial.dcp_event_filter.dcp_event_filter;

public class EventConfig {

	public String model;
	public String queueName;
	
	public EventConfig(String model, String queueName) {
		this.model = model;
		this.queueName = queueName;
	}
}
