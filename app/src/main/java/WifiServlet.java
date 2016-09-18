import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
 
public class WifiServlet extends HttpServlet
{
    public static final String userDir = System.getProperty("user.home");
    public static final String outDir = userDir+File.separator+".esp8266";
    public static final File dir = new File(outDir);
    public static final Map<String,File> outFiles = new HashMap<String,File>();
    public static final String DELIM = "|";
    public static final DateFormat dfDay = new SimpleDateFormat("YYYYMMDD");
    public static final DateFormat dfTime = new SimpleDateFormat("HHmmss");
    public static File file;

    static {
	try {
	  initServlet();
	} catch (Exception e) {
	  e.printStackTrace();
	}
    }

    public static void initServlet() throws IOException {
	dir.mkdirs();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        //System.out.println("userDir="+userDir);
	String content = "";
	Date now = new Date();
	String day = dfDay.format(now);
	String time = dfTime.format(now);
	String ipAddress = request.getRemoteAddr();
	String baseUrl = getBaseUrl(request);
        //System.out.println("request="+request);
        //System.out.println("request.getPathInfo()="+request.getPathInfo());
        //System.out.println("request.getServletPath()="+request.getServletPath());
        //System.out.println("request.getQueryString()="+request.getQueryString());
	String chipId = request.getPathInfo();
	if (chipId==null || chipId.trim().length()==0) {
	  content = "<ul>";
	  for (String outFile : outFiles.keySet()) {
	    content += "<li><a href='"+baseUrl+"/"+outFile.substring(9)+"'>"+outFile+".txt</a></li>";
	  }
	  content += "</ul>";
	  writeResponse(request,response,content);
	  return ;
	}
	chipId = chipId.substring(1);
	String chipKey = day+"_"+chipId;
	if (outFiles.containsKey(chipKey)) {
	  file = outFiles.get(chipKey);
	} else {
	  file = new File(dir,chipKey+".txt");
	  if (!file.exists()) {
	    file.createNewFile();
	  }
	  outFiles.put(chipKey,file);
	}
	String queryString = request.getQueryString();
	if (queryString!=null && queryString.trim().length()>0) {
	  String[] queryParts = request.getQueryString().split("&");
	  String heap ="";
	  String distance = "";
	  for (int i=0; i<queryParts.length; i++) {
	    String[] paramParts = queryParts[i].split("=");
	    if (paramParts.length>1) {
	      if (paramParts[0].equals("val")) { distance = paramParts[1]; }
	      if (paramParts[0].equals("heap")) { heap = paramParts[1]; }
	    }
	  }
	  if (heap.length()>0 && distance.length()>0) {
	    String result = day+DELIM+time+DELIM+ipAddress+DELIM+heap+DELIM+distance;
            System.out.println("result="+chipId+DELIM+result);
	    PrintWriter fout = null;
	    try {
	      fout = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
	      fout.append(result+"\n");
	    } finally {
	      if (fout!=null) {
	        fout.flush();
	        fout.close();
	      }
	    }
	    content = "val="+request.getParameter("val")+"<br/>";
	    content += "session=" + request.getSession(true).getId();
	  }
	} else {
	  content = "<div>"+getLastNLogLines(file,60)+"</div>";
	}
	
	writeResponse(request,response,content);
    }

    public static String getBaseUrl(HttpServletRequest request) {
	String url = request.getRequestURL().toString();
	String baseUrl = url.substring(0, url.indexOf("/"));
	baseUrl = baseUrl+request.getServletPath();
	return baseUrl;
    }

    public static void writeResponse(HttpServletRequest request, HttpServletResponse response, String content) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        response.setStatus(HttpServletResponse.SC_OK);
        out.println("<h1><a href="+getBaseUrl(request)+">Wifi Servlet</a></h1>");
        out.println(content);
    }

    static public String getContents(File readFile, int linesToRead) {
      StringBuilder contents = new StringBuilder();
    
      try {
        BufferedReader input =  new BufferedReader(new FileReader(readFile));
        try {
          String line = null; 
  	  int i=0;
          while (( line = input.readLine()) != null){
            contents.append(line);
            contents.append("<br/>");
	    if (++i>=linesToRead) break;
          }
        }
        finally {
          input.close();
        }
      }
      catch (IOException ex){
        ex.printStackTrace();
      }
    
      return contents.toString();
    }

    public String getLastNLogLines(File readFile, int linesToRead) {
      StringBuilder s = new StringBuilder();
      try {
        Process p = Runtime.getRuntime().exec("tail -"+linesToRead+" "+file);
        BufferedReader input = new BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
        String line = null;
        //Here we first read the next line into the variable
        //line and then check for the EOF condition, which
        //is the return value of null
        while((line = input.readLine()) != null){
            s.insert(0,line+"<br/>");
        }
      } catch (java.io.IOException e) {
        e.printStackTrace();
      }
      return s.toString();
    }
 
}
