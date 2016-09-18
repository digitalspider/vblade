package au.com.digitalspider.handler;

import java.net.NoRouteToHostException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.velocity.app.VelocityEngine;

import au.com.digitalspider.Util;

public class BroadcastOutputHandler extends AbstractOutputHandler {

	public static String DELIM = ",";
	public static String PARAM_DESTINATION = "broadcast.dest";
	protected String[] destinations;

	@Override
	public void init(HttpServletRequest request, HttpServletResponse response, VelocityEngine ve, String ipAddress, String chipId, String type, String heap, String... values) {
		super.init(request, response, ve, ipAddress, chipId, type, heap, values);
		
		String destParam = properties.getProperty(PARAM_DESTINATION);
		if (destParam!=null && destParam.trim().length()>0) {
			destinations = destParam.split(DELIM);
		}
	}
	
	@Override
	public String call() throws Exception {
		log.debug("call() at "+this);
		
		if (heap.length() > 0 && values!=null && values.length>0 && values[0].trim().length() > 0) {
			if (destinations != null && request!=null) {
				for (String destination : destinations) {
					String url = request.getRequestURL().toString();
					String host = Util.getHost(url);
					if (!host.equalsIgnoreCase(destination)) {
						log.debug("host="+host+" destination="+destination);
						String newUrl = url.replace(host, destination);
						CloseableHttpResponse response = null;
						try {
							CloseableHttpClient client = HttpClients.createDefault();
							HttpGet httpget = new HttpGet(newUrl);
							response = client.execute(httpget);
							StatusLine statusLine = response.getStatusLine(); 
							int statusCode = statusLine.getStatusCode();
							log.debug("statusCode="+statusCode);
						} catch(NoRouteToHostException e) {
							log.error("Error connecting to url "+newUrl+". NoRouteToHostException");
						} catch(Exception e) {
							log.error("Error connecting to url "+newUrl,e);
						} finally {
							if (response!=null) { response.close(); }
						}
					}
				}
			}
		}
		return null;
	}

}
