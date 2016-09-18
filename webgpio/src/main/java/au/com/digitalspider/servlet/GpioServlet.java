package au.com.digitalspider.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.GpioProvider;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;

import au.com.digitalspider.Util;

public class GpioServlet extends HttpServlet {

	private static final long serialVersionUID = -2464987655294189911L;
	private static boolean initialised = false;
	
	public enum PinRequest { VALUE, DIRECTION, EDGE, TOGGLE, PWM };
	public enum PinDirection { IN, OUT };
	public enum PinEdge { NONE, RISING, FALLING, BOTH };
  
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,ServletException {  

		try {
			System.out.println("pathinfo="+request.getPathInfo());
			String[] pathParts = Util.getPathParts(request);
			System.out.println("PathParts="+pathParts);
			
			initPi4j();
			
			int pin = -1;
			PinRequest pinRequest = null;
			String valueParam = null;
			int pinValue = -1;
			int pinDuty = 512;
			int pinCycle = 1024;
			PinEdge edge = null;
			PinDirection dir = null;
			for (int i=0; i<pathParts.length; i++) {
				switch (i) {
					case 0:
						try {
							pin = Integer.parseInt(pathParts[i]);
							if (pin <0 || pin> Gpio.NUM_PINS) {
								throw new Exception("pin value "+pin+" outside of range 0 to "+Gpio.NUM_PINS);
							}
						} catch (Exception e) {
							Util.writeError(request,response,"Invalid pin input: "+e.getMessage());
							return;
						}
						break;
					case 1:
						try {
							pinRequest = PinRequest.valueOf(pathParts[i].toUpperCase());
						} catch (Exception e) {
							Util.writeError(request,response,"Invalid pin request input: "+e.getMessage());
							return;
						}
						break;
					case 2:
						try {
							valueParam = pathParts[i];
							validateValueParam(pin,pinRequest,valueParam, pathParts);
							switch (pinRequest) {
								case VALUE:
									pinValue = Integer.parseInt(valueParam);
									break;
								case PWM:
									pinDuty = Integer.parseInt(valueParam);
									if (pathParts.length>i) {
										pinCycle = Integer.parseInt(pathParts[i+1]);
									}
									break;
								case DIRECTION:
									dir = PinDirection.valueOf(valueParam.toUpperCase());
									break;
								case EDGE:
									edge = PinEdge.valueOf(valueParam.toUpperCase());
									break;
								case TOGGLE:
									break;
							}
						} catch (Exception e) {
							Util.writeError(request,response,"Invalid pin request input: "+e.getMessage());
							return;
						}
						break;
					default:
						break;
				}
			}
			
			response.setContentType("text/html");  
	
			writeResponse(request,response,pin,pinRequest,valueParam,pinValue,edge,dir,pinCycle,pinDuty);
		} catch (Exception e) {
			Util.writeError(request, response, e);
		}
	}
	
	private void initPi4j() {
		System.out.println("initPi4j() called");
		if (!initialised) {
			System.out.println("initPi4j() START");
			GpioController gpio = GpioFactory.getInstance();
			System.out.println("gpio="+gpio);
			GpioProvider provider = GpioFactory.getDefaultProvider();
			System.out.println("provider="+provider);
			System.out.println("pins provision start");
			Integer[] includePins = new Integer[] {0,1,2,3,4,5,6,7,10,11,21,22,24,26,27};
			for (int i=0; i<includePins.length; i++) {
				String pinId = ""+includePins[i];
				System.out.println("pinId="+pinId);
				Pin gpiopin = RaspiPin.getPinByName("GPIO "+pinId);
				System.out.println("pin="+gpiopin);
				gpio.provisionDigitalOutputPin(gpiopin);
			}
			System.out.println("pins provision done");
			initialised=true;
			System.out.println("initPi4j() DONE");
		}
	}
	
	private void validateValueParam(int pin, PinRequest pinRequest, String valueParam, String[] pathParts) throws Exception {
		switch (pinRequest) {
			case TOGGLE:
				break;
			case VALUE:
				int value = -1;
				try {
					value = Integer.parseInt(valueParam);
				} catch (Exception e) {
					throw new Exception("Parameter "+pinRequest.name().toLowerCase()+" for pin "+pin+" must be numeric!");
				}
				if (value<0 || value>1) {
					throw new Exception("Parameter "+pinRequest.name().toLowerCase()+" for pin "+pin+" must be 0 or 1!");
				}
				break;
			case PWM:
				int duty = -1;
				int cycle = -1;
				try {
					duty = Integer.parseInt(valueParam);
				} catch (Exception e) {
					throw new Exception("Parameter "+pinRequest.name().toLowerCase()+" for pin "+pin+" must be numeric!");
				}
				if (duty<0 || duty>4096) {
					throw new Exception("Parameter "+pinRequest.name().toLowerCase()+" for pin "+pin+" must be between 0 and 4096!");
				}
				try {
					if (pathParts.length>3) {
						cycle = Integer.parseInt(pathParts[3]);
					}
				} catch (Exception e) {
					throw new Exception("Parameter "+pinRequest.name().toLowerCase()+" cycle for pin "+pin+" must be numeric!");
				}
				if (cycle<0 || cycle>4096) {
					throw new Exception("Parameter "+pinRequest.name().toLowerCase()+" cycle for pin "+pin+" must be between 0 and 4096!");
				}
				break;
			case DIRECTION:
				try {
					PinDirection dir = PinDirection.valueOf(valueParam.toUpperCase());
					if (dir == null) {
						throw new Exception("Parameter "+pinRequest.name().toLowerCase()+" for pin "+pin+" must be in or out!");
					}
				} catch (Exception e) {
					throw new Exception("Parameter "+pinRequest.name().toLowerCase()+" for pin "+pin+" must be in or out!");
				}
				break;
			case EDGE:
				try {
					PinEdge edge = PinEdge.valueOf(valueParam.toUpperCase());
					if (edge == null) {
						throw new Exception("Parameter "+pinRequest.name().toLowerCase()+" for pin "+pin+" must be rising,falling or both!");
					}
				} catch (Exception e) {
					throw new Exception("Parameter "+pinRequest.name().toLowerCase()+" for pin "+pin+" must be rising,falling or both!");
				}
				break;
			default:
				throw new Exception("Parameter "+pinRequest.name().toLowerCase()+" for pin "+pin+" is INVALID!");
		}
	}

	private void writeResponse(HttpServletRequest request, HttpServletResponse response, int pin, PinRequest pinRequest, String valueParam, int pinValue, PinEdge edge, PinDirection dir, int pinCycle, int pinDuty) throws IOException {
		PrintWriter out=response.getWriter();  
		out.println("<html><body>");  
		out.println("<h2><a href='/'>Home</a> | <a href='/gpio'>WebGpio</a> | <a href='/gpio'>WebI2C</a> | <a href='/esp8266'>Esp8266</a></h2>");
		out.println("<p>Pin=<b>"+pin+"</b> and PinRequest=<b>"+pinRequest+"</b></p>");
		String action = null;
		if (pinRequest!=null) {
			action = pinRequest.name().toLowerCase();
		}
		if (valueParam!=null) {
			switch (pinRequest) {
				case TOGGLE:
					break;
				case VALUE:
					action += "="+pinValue;
					break;
				case PWM:
					action += "="+pinDuty+"/"+pinCycle;
					break;
				case DIRECTION:
					action += "="+dir.name().toLowerCase();
					break;
				case EDGE:
					action += "="+edge.name().toLowerCase();
					break;
			}
			out.println("<p>Action was to set "+action+"</b></p>");
		}
		out.println("<h1>PINS</h1>");
		out.println("<table border='1'>");
		int COLS=5;
		GpioController gpio = GpioFactory.getInstance();
		GpioProvider provider = GpioFactory.getDefaultProvider();
		Collection<GpioPin> pins = gpio.getProvisionedPins();
		System.out.println("pins="+pins);
		
		int i=0;
		for (GpioPin gpiopin : pins) {
			boolean selected = false;
			if (i%COLS==0) {
				out.println("<tr>");
			}
			//System.out.println("gpiopin="+gpiopin);
			//System.out.println("gpiopin.getPin()="+gpiopin.getPin());			
			String mode = provider.getMode(gpiopin.getPin()).toString();
			String state = provider.getState(gpiopin.getPin()).toString();
			int pedge = GpioUtil.getEdgeDetection(gpiopin.getPin().getAddress());
			Integer pinId = Integer.parseInt(gpiopin.getPin().getName().substring(5));
			if (pinId == pin) {
				System.out.println("selected!");
				selected = true;
			}
			out.println("<td>");
			if (selected && pinRequest!=null) {
				if (gpiopin instanceof GpioPinDigitalOutput) {
					if (pinRequest==PinRequest.TOGGLE) {
						System.out.println("Toggle");
						((GpioPinDigitalOutput)gpiopin).toggle();
					} else if (pinRequest==PinRequest.PWM) {
							System.out.println("pwm duty="+pinDuty+", cycle="+pinCycle);
							int count=0;
							while (count<pinCycle*10) {
								int x = ++count % pinCycle;
								if (x<pinDuty) {
									((GpioPinDigitalOutput)gpiopin).high();
								} else {
									((GpioPinDigitalOutput)gpiopin).low();
								}
							}
					} else if (pinRequest==PinRequest.VALUE && action.contains("=")) {
						PinState newState = PinState.getState(pinValue);
						System.out.println("newState="+newState);					
						gpio.setState(newState, (GpioPinDigitalOutput)gpiopin);
						System.out.println("setState() done");
					}
					state = provider.getState(gpiopin.getPin()).toString();;
				}
				out.println("<strong>");
			}
			out.println("Pin="+pinId+" ["+mode.replace("PUT","")+"] [<a href=\"/gpio/"+pinId+"/toggle\">"+state+"</a>] ["+pedge+"]");
			if (selected) {
				out.println("</strong>");
			}
			out.println("</td>");
			//if (!gpiopin.equals(RaspiPin.GPIO_00)) {
			//	System.out.println("value="+provider.getValue(gpiopin.getPin()));
			//}
			
			if (i%COLS==COLS-1) {
				out.println("</tr>");
			}
			i++;
		}
		if (Gpio.NUM_PINS%COLS==COLS-1) {
			out.println("</tr>");
		}
		out.println("</table>");
		out.println("</body></html>");
	}
	
}
