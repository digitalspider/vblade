package au.com.digitalspider.handler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.app.VelocityEngine;

public class RawOutputHandler extends AbstractOutputHandler {
	
	public static final String DELIM = "|";
	
	protected String template;
	protected String fileSuffix;
	

	@Override
	public void init(HttpServletRequest request, HttpServletResponse response, VelocityEngine ve, String ipAddress, String chipId,
			String type, String heap, String... values) {
		super.init(request, response, ve, ipAddress, chipId, type, heap, values);
		template = properties.getProperty(PARAM_TEMPLATE,DEFAULT_TEMPLATE);
		fileSuffix = properties.getProperty(PARAM_FILESUFFIX,DEFAULT_FILESUFFIX);
	}
	
	@Override
	public String call() throws Exception {
		log.debug("call() at "+this);
		
		if (heap.length() > 0 && values!=null && values.length>0 && values[0].trim().length() > 0) {
			
			Date now = new Date();
			String day = DFDAY.format(now);
			String time = DFTIM.format(now);
			
			String result = day+DELIM+time+DELIM+ipAddress+DELIM+heap+DELIM+values[0];
			for (int i=1; i<values.length; i++) {
				String value = values[i];
				if (value!=null && value.trim().length()>0) {
					result+=DELIM+value;
				}
			}
			log.debug("result="+result);
			
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
		
		return null;
	}
	
	@Override
	public String toString() {
		return super.toString()+" template="+template+" fileSuffix="+fileSuffix;
	}

}
