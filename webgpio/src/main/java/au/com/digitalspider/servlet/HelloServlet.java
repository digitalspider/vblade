package au.com.digitalspider.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

public class HelloServlet extends HttpServlet {

	private static final long serialVersionUID = -2464987655294189911L;
  
	public void service(ServletRequest req,ServletResponse res) throws IOException,ServletException {  

		res.setContentType("text/html");  
		  
		PrintWriter out=res.getWriter();  
		out.print("<html><body>");  
		out.print("<b>Welcome. This is the HelloServlet</b>");  
		out.print("</body></html>");  
	  
	}
	
}
