package au.com.digitalspider.handler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import au.com.digitalspider.Util;

public class InterruptOutputHandler extends RawOutputHandler {

	private static Map<String,Float[]> historyDataMap = new HashMap<String,Float[]>();
	
	public static String PARAM_HISTORY_LENGTH = "interrupt.history.length";
	public static String DEFAULT_HISTORY_LENGTH = "10";
	
	protected int historySize;

	@Override
	public void init(HttpServletRequest request, HttpServletResponse response, VelocityEngine ve, String ipAddress, String chipId, String type, String heap, String... values) {
		DEFAULT_FILESUFFIX = "_int.txt";
		super.init(request, response, ve, ipAddress, chipId, type, heap, values);
		historySize = Integer.parseInt(properties.getProperty(PARAM_HISTORY_LENGTH,DEFAULT_HISTORY_LENGTH));
	}
	
	@Override
	public String call() throws Exception {
		log.debug("call() at "+this);
		
		if (heap.length() > 0 && values!=null && values.length>0 && values[0].trim().length() > 0) {
			Float[] historyData = historyDataMap.get(chipId);
			if (historyData==null) {
				historyData = new Float[historySize];
				historyDataMap.put(chipId, historyData);
			}
			String distanceString = values[0];
			Float distance = Float.parseFloat(distanceString);
			Float average = 0.0F;
			
			if (distance>0) {
				// Calculate avg and also update(shift) the history data
				for (int i=0; i<historyData.length; i++) {
					Float value = historyData[i];
					average += value!=null && value>0 ? value : 0.0F;
					if (i>0) {
						historyData[i-1] = historyData[i];
					}
				}
				historyData[historyData.length-1] = distance;
				average = average/historyData.length;
			}
			
			if (historyData[0]==null || historyData[0].equals(0)) {
				// not yet initialised return
				return null;
			}
			
			VelocityContext context = getVelocityContext();
			context.put("avg",average);
			String result = Util.useVelocityTemplate(ve,template,context);
			log.debug("result="+result);
			
			if (result!=null && result.trim().length()>0) {
				
				// Get the file requested, or create a new file
				File file = getOutputFile(fileSuffix);
				log.debug("file="+file.getAbsolutePath());
				
				PrintWriter fout = null;
				try {
					fout = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
					fout.append(result + "\n");
				} finally {
					if (fout != null) {
						fout.flush();
						fout.close();
					}
				}
			}
		}
		
		return null;
	}
}
