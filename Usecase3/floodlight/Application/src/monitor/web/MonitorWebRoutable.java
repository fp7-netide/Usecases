package net.floodlightcontroller.monitor.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

/**
 * Creates a router to handle the Monitor web URIs
 * @author K.Phemius<br>
 * DT/CEA/TAI Lab<br>
 * Copyright (c) 2014 Thales Communications & Security<br>
 * 4 av. des Louvresses - 92230 Gennevilliers - France<br>
 * All rights reserved
 *
**/
public class MonitorWebRoutable implements RestletRoutable {
	/**
     * Create the Restlet router and bind to the proper resources.
     */
    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/{query}", MonitorResource.class);
        return router;
    }
    
    /**
     * Set the base path for the Monitor
     */
    @Override
    public String basePath() {
        return "/wm/monitor";
    }
}