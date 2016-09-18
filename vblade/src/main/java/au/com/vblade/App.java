package au.com.vblade;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.text.DecimalFormat;
import java.io.IOException;
import java.math.RoundingMode;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.gpio.event.GpioPinListener;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;
import com.pi4j.wiringpi.GpioInterrupt;
import com.pi4j.wiringpi.GpioInterruptCallback;

public class App 
{
    private final static float SOUND_SPEED = 340.29f;  // speed of sound in m/s
    private final static int TIMEOUT = 2100;
    private GpioController gpio;
    
    public App(GpioController gpio) {
		this.gpio = gpio;
	}
	
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
		try {
			App app = new App(GpioFactory.getInstance());
			GpioController gpio = app.getController();
			System.out.println("gpio="+gpio);
			// Pin Declaration
			GpioPinDigitalOutput pin0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "PIN0(88)", PinState.LOW);
			GpioPinDigitalOutput pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "PIN1(87)", PinState.LOW);
			GpioPinDigitalOutput pin2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "PIN2(116)", PinState.LOW);
			GpioPinDigitalOutput pin3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "PIN3(115)", PinState.LOW);
			GpioPinDigitalOutput pin4 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "PIN4(104) - Trig", PinState.LOW);
			GpioPinDigitalInput pin5 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_05, "PIN5(102) - Echo");
			GpioPinDigitalInput pin23 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_23, "PIN5(108) - Encode1");
			GpioPinDigitalInput pin24 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_24, "PIN5(97) - Encode2");
			System.out.println("pins loaded");
			
			// Pins for toggle
			List<GpioPinDigitalOutput> pins = new ArrayList<GpioPinDigitalOutput>();
			pins.add(pin0);
			pins.add(pin1);
			pins.add(pin2);
			pins.add(pin3);
			
			// Pins for hcrf04
			GpioPinDigitalOutput pinTrig = pin4;
			GpioPinDigitalInput pinEcho = pin5;
			
			// Pins for encoder
			GpioPinDigitalInput pinEn1 = pin23;
			GpioPinDigitalInput pinEn2 = pin24;

			// Start application
			System.out.println("APP START");
			//app.togglePins(pins, 4);
			//app.i2cSRF10(10);
			//app.i2cGY45(50);
			//app.hcrf04(pinTrig, pinEcho, pin0, pin1, 100);
			app.encodeInput(pinEn1, pinEn2, 30);
			System.out.println("APP DONE");
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println( "DONE" );
    }
    
    public int forwardCount = 0;
	public int backwardCount = 0;
	public long rpm = 0;
	public long timeold = 0;
	
    public void encodeInput(final GpioPinDigitalInput pin1, final GpioPinDigitalInput pin2, int iterations) throws Exception {
		try {
			System.out.println("Reading encoder START");
			long beginTime = System.nanoTime();
			DecimalFormat df = new DecimalFormat("#,###,###");
			Collection<GpioPinListener> listeners = pin1.getListeners();
			System.out.println("Listeners on "+pin1+": "+listeners.size());
			if (listeners.isEmpty()) {
				System.out.println("* No listeners on "+pin1);
			}
			for (GpioPinListener listener : listeners) {
				System.out.println("* listener="+listener);
			}
			System.out.println("<--Pi4J--> GPIO interrupt test program");

			// setup wiringPi
			if (Gpio.wiringPiSetup() == -1) {
				System.out.println(" ==>> GPIO SETUP FAILED");
				return;
			}
			System.out.println(" ==>> GPIO SETUP DONE");
			System.out.println(" ==>> GPIO PinSupported="+GpioUtil.isPinSupported(23));
			System.out.println(" ==>> GPIO PinExported="+GpioUtil.isExported(23));
			System.out.println(" ==>> GPIO PinDirection="+GpioUtil.getDirection(23));
			System.out.println(" ==>> GPIO PinEdgeDetection="+GpioUtil.getEdgeDetection(23));

/*
			GpioInterrupt.enablePinStateChangeCallback(108);
			System.out.println("GpioInterrupt.enablePinStateChangeCallback(108) Done");
			pin1.addListener( new GpioPinListenerDigital() {
				public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
					System.out.println("event="+event);
					if (pin1.getState() == pin2.getState() ) {
						forwardCount++;
					} else {
						backwardCount++;
					}
					System.out.println("forwardCount="+forwardCount+" backwardCount="+backwardCount);
					if (forwardCount>=20) {
						//Update RPM every 20 counts, increase this for better RPM resolution,
						//decrease for faster update
						rpm = 30*1000/(System.currentTimeMillis() - timeold)*forwardCount;
						timeold = System.currentTimeMillis();
						forwardCount = 0;
						System.out.println("rpm="+rpm);
					}
				}
			});
*/
			System.out.println("New listeners on "+pin1+": "+listeners.size());
			for (GpioPinListener listener : pin1.getListeners()) {
				System.out.println("* listener="+listener);
			}
			Gpio.wiringPiISR(23, Gpio.INT_EDGE_RISING, new GpioInterruptCallback() {
				@Override
				public void callback(int pin) {
					System.out.println(" ==>> GPIO PIN " + pin + " - INTERRUPT DETECTED");
				}
			});
			for (int i=1; i<iterations+1; i++) {
				System.out.println(df.format((System.nanoTime()-beginTime)/1000)+": "+i+": "+pin1.getState()+" : "+pin2.getState());
				Thread.sleep(0,500);
			}
			System.out.println("Reading encoder DONE");
			// wait for user to exit program
			System.console().readLine("Press <ENTER> to exit program.\r\n");
		} finally {
			pin1.removeAllListeners();
		}
	}
    
    public void togglePins(List<GpioPinDigitalOutput> pins, int iterations) throws Exception {

		for (int i=1; i<iterations+1; i++) {
        	try {
        		System.out.println(i+": Waiting...");
        		// wait 1 second
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			for (GpioPinDigitalOutput pin : pins) {
				pin.toggle();
				System.out.println("pin="+pin+" is "+pin.getState());
			}
        }
        
        // Set all pins low
        for (GpioPinDigitalOutput pin : pins) {
			pin.low();
		}
	}
    
    public void i2cSRF10(int iterations) throws Exception {
		System.out.println("Starting SRF10 reading:");

		I2CBus bus = null;
		try {
			//get i2c bus
			int busId = I2CBus.BUS_1;
			System.out.println("Connecting to busId: "+busId);
			bus = I2CFactory.getInstance(busId);
			System.out.println("Connected to bus "+bus.getFileName()+" OK!");

			//get device itself
			int address = 0x70;
			System.out.println("Connecting to device address: "+address);
			I2CDevice device = bus.getDevice(address);
			System.out.println("Connected to device OK!");

			//start sensing, using config registries 6B  and 6C
			int commandRegister = 0x00;
			int maxGainRegister = 0x01;
			int rangeRegister = 0x02;
			int rangeHighByte = 0x02;
			int rangeLowByte = 0x03;
			byte cmdInches = 0x50;
			byte cmdCentimeters = 0x51;
			byte cmdMillis = 0x52;
			int size = 2;
			byte[] buffer = new byte[size];
			int offset = 0;
			
			// Read version
			int readData = device.read(commandRegister);
			System.out.println("version="+readData);

			// Set distance to 10 (0x0a) which = 43x10+43=47.3cm
			byte distance = 0x30;
			device.write(rangeRegister,distance);
			double distanceInCm = (4.3*distance)+4.3;
			DecimalFormat df = new DecimalFormat("#.00");
			df.setRoundingMode(RoundingMode.CEILING);
			System.out.println("Distance set to "+df.format(distanceInCm)+"cm");

			// Read sensors in centimeters
			for (int i=1; i<iterations+1; i++) {
				device.write(commandRegister, cmdCentimeters);
				Thread.sleep(70);
				int readHigh = device.read(rangeHighByte);
				int readLow = device.read(rangeLowByte);
				int value = (readHigh << 8) | (readLow & 0x00ff);
				System.out.println(i+":res="+readHigh+":"+readLow+":"+value);
				
				Thread.sleep(250);
			}
		
		} finally {
			if (bus != null) { bus.close(); }
		}
		System.out.println("Configuring Device OK!");		
	}

    public void i2cGY45(int iterations) throws Exception {
		System.out.println("Starting GY45 reading:");
		
		I2CBus bus = null;
		try {
			//get i2c bus
			int busId = I2CBus.BUS_1;
			System.out.println("Connecting to busId: "+busId);
			bus = I2CFactory.getInstance(busId);
			System.out.println("Connected to bus "+bus.getFileName()+" OK!");

			//get device itself
			int address = 0x1C;
			System.out.println("Connecting to device address: "+address);
			I2CDevice device = bus.getDevice(address);
			System.out.println("Connected to device OK!");

			//Define a few of the registers that we will be accessing on the MMA8452
			int OUT_X_MSB = 0x01;
			int OUT_X_LSB = 0x02;
			int OUT_Y_MSB = 0x03;
			int OUT_Y_LSB = 0x04;
			int OUT_Z_MSB = 0x05;
			int OUT_Z_LSB = 0x06;
			int XYZ_DATA_CFG = 0x0E;
			int WHO_AM_I = 0x0D;
			int WHO_AM_I_CHECK = 0x1A;
			int CTRL_REG1 = 0x2A;
			
			int GSCALE=2;

			byte cmdInches = 0x50;
			byte cmdCentimeters = 0x51;
			byte cmdMillis = 0x52;
			int size = 2;
			byte[] buffer = new byte[size];
			int offset = 0;
			
			// Read version
			int readData = device.read(WHO_AM_I);
			System.out.println("version="+readData);
			
			/*
			if (readData != WHO_AM_I_CHECK) {
				System.err.println("WHO_AM_I register: "+WHO_AM_I+" returned "+readData+". Expected "+WHO_AM_I_CHECK);
				System.exit(1);
			}
			*/

			// Set to standby
			byte c = (byte)device.read(CTRL_REG1);
			byte c2 = (byte)(c & ~(0x01));
			System.out.println("Setting to standby. c="+c+" c2="+c2);
			device.write(CTRL_REG1, c2); //Clear the active bit to go into standby

			// Set up the full scale range to 2, 4, or 8g.
			byte fsr = (byte)GSCALE;
			if(fsr > 8) fsr = 8; //Easy error check
			fsr >>= 2; // Neat trick, see page 22. 00 = 2G, 01 = 4A, 10 = 8G
			System.out.println("Set GScale="+GSCALE+" fsr="+fsr);
			device.write(XYZ_DATA_CFG, fsr);
  
			// Set to active
			c = (byte)device.read(CTRL_REG1);
			c2 = (byte)(c | 0x01);
			System.out.println("Setting to active. c="+c+" c2="+c2);
			device.write(CTRL_REG1, c2); //Set the active bit to begin detection
			
			Thread.sleep(100);
  
			// Read sensors in centimeters
			for (int i=1; i<iterations+1; i++) {
				//device.write((byte)commandRegister,cmdCentimeters);
				//Thread.sleep(70);
				int readHighX = device.read((byte)OUT_X_MSB);
				int readLowX = device.read((byte)OUT_X_LSB);
				int valueX = (readHighX << 8) | (readLowX & 0x00ff);
				valueX >>= 2; //The registers are left align, here we right align the 14-bit integer
				/*
				if (readHighX > 0x7F) { // If the number is negative, we have to make it so manually (no 14-bit data type)
					valueX = ~valueX + 1;
					valueX *= -1;  // Transform into negative 2's complement #
				}
				valueX = (float) valueX / ((1<<12)/(2*GSCALE));
				*/
				int readHighY = device.read((byte)OUT_Y_MSB);
				int readLowY = device.read((byte)OUT_Y_LSB);
				int valueY = (readHighY << 8) | (readLowY & 0x00ff);
				valueY >>= 2; //The registers are left align, here we right align the 14-bit integer
				int readHighZ = device.read((byte)OUT_Z_MSB);
				int readLowZ = device.read((byte)OUT_Z_LSB);
				int valueZ = (readHighZ << 8) | (readLowZ & 0x00ff);
				valueZ >>= 2; //The registers are left align, here we right align the 14-bit integer
				System.out.println(i+":res x="+readHighX+":"+readLowX+":"+valueX+": y="+readHighY+":"+readLowY+":"+valueY+": z="+readHighZ+":"+readLowZ+":"+valueZ);
				
				Thread.sleep(500);
			}
		} finally {
			if (bus != null) { bus.close(); }
		}
		System.out.println("Configuring Device OK!");		
	}
	
	private int readRegister(I2CDevice device, byte addressToRead) throws IOException {
		byte[] data = new byte[1];
		readRegister(device, addressToRead, 1, data);
		return (int)data[0];
	}
	
	private void readRegister(I2CDevice device, byte addressToRead, int bytesToRead, byte[] dest) throws IOException
	{
		device.write(addressToRead);

		// Wire.requestFrom(MMA8452_ADDRESS, bytesToRead); //Ask for bytes, once done, bus is released by default

		//while(Wire.available() < bytesToRead); //Hang out until we get the # of bytes we expect

		for(int x = 0 ; x < bytesToRead ; x++) {
			dest[x] = (byte)device.read();    
		}
	}

	public void hcrf04(GpioPinDigitalOutput pinTrig, GpioPinDigitalInput pinEcho, GpioPinDigitalOutput pin1, GpioPinDigitalOutput pin2, int iterations) throws Exception {

		long beginTime = System.nanoTime();
		DecimalFormat df = new DecimalFormat("#,###,###");
		
		for (int i=1; i<iterations+1; i++) {
			// The sensor is triggered by a HIGH pulse of 10 or more microseconds.
			// Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
			pinTrig.low();
			Thread.sleep( 0, 5000 );
			pinTrig.high();
			Thread.sleep( 0, 10000 );
			pinTrig.low();
			//System.out.println(System.nanoTime()+": request measure");

			// Read the signal from the sensor: a HIGH pulse whose
			// duration is the time (in microseconds) from the sending
			// of the ping to the reception of its echo off of an object.
			//duration = pulseIn(echoPin, HIGH); // pulseIn is an Arduino function!
			int countdown = TIMEOUT;
			while( pinEcho.isLow() && countdown > 0 ) {
				countdown--;
			}
			if( countdown <= 0 ) {
				throw new TimeoutException( "Timeout waiting for signal start" );
			}
			
			countdown = TIMEOUT;
			long start = System.nanoTime();
			while( pinEcho.isHigh() && countdown > 0 ) {
				countdown--;
			}
			long end = System.nanoTime();
			if( countdown <= 0 ) {
				throw new TimeoutException( "Timeout waiting for signal end" );
			}

			long duration = (long)Math.ceil( ( end - start ) / 1000.0 );  // Return micro seconds

			// convert the time into a distance
			float cm = duration * SOUND_SPEED / ( 2 * 10000 );

			System.out.println(df.format((System.nanoTime()-beginTime)/1000)+": "+i+": duration="+duration+"micros distance="+cm+"cm");

			if (cm < 50) {
				pin1.high();
			} else {
				pin1.low();
			}
			if (cm < 25) {
				pin2.high();
			} else {
				pin2.low();
			}

			Thread.sleep( 250 );
		}
	}
	
	public GpioController getController() {
		return gpio;
	}
	
	public void setController(GpioController gpio) {
		this.gpio = gpio;
	}
}
