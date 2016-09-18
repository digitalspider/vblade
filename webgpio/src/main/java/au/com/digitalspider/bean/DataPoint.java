package au.com.digitalspider.bean;

import java.util.ArrayList;
import java.util.List;

public class DataPoint {

	public String date;
	public String time;
	public String ip;
	public String heap;
	public String value;
	public List<String> values = new ArrayList<String>();
	
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getHeap() {
		return heap;
	}
	public void setHeap(String heap) {
		this.heap = heap;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public List<String> getValues() {
		return values;
	}
	public void setValues(List<String> values) {
		this.values = values;
	}

}
