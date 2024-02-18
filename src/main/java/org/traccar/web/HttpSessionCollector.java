package org.traccar.web;

import java.util.*;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.traccar.api.resource.SessionResource;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

public class HttpSessionCollector implements HttpSessionListener {
    //private static final Logger LOGGER = LoggerFactory.getLogger(HttpSessionCollector.class);
    private static final Map<String, HttpSession> SESSIONS = new HashMap<String, HttpSession>();

    public HttpSessionCollector() {
        super();
        //System.out.println("---------------------------HttpSessionCollector");
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        SESSIONS.put(session.getId(), session);
        //System.out.println("---------------------------HttpSessionCollector > sessionCreatedr : " + session.getId());
        //LOGGER.warn("sessionCreated", session.getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        SESSIONS.remove(event.getSession().getId());
        //System.out.println("---------------------------HttpSessionCollector > sessionDestroyed : " + event.getSession().getId());
        //LOGGER.warn("sessionDestroyed", event.getSession().getId());
    }

    public static HttpSession find(String sessionId) {
        //System.out.println("---------------------------HttpSessionCollector > find : " + sessionId);
        return (HttpSession) SESSIONS.get(sessionId);
    }

    public static int noOfActiveSessions() {
        int count = 0;
        for (HttpSession sessionValue : SESSIONS.values()) {
            if (sessionValue != null && sessionValue.getAttribute(SessionResource.USER_ID_KEY) != null) {
                count = count + 1;
            }
        }
        return count;
    }

}
