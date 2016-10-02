package mo.train.info.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.jboss.resteasy.plugins.server.sun.http.HttpContextBuilder;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import mo.train.info.util.*;

@SuppressWarnings("restriction")
@Path("/train/info")
public class TrainInfoService {
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException {
		TrainInfoService t = new TrainInfoService();
//		System.out.println(t.getStationName("杭州"));
//		System.out.println(t.getTrainNo("2016-05-03", "Z88"));
//		String city[] = {"杭州", "西安", "大连", "兰州", "厦门", "北京", "上海", "嘉兴"
//				, "乌鲁木齐", "呼和浩特", "咸阳", "南京", "贵阳", "柳州", "合肥", "郑州"};
//		Random rand = new Random(System.currentTimeMillis());
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//		for(int i = 0; i < 5; i ++) {
//			Calendar cal = Calendar.getInstance();
//			cal.add(Calendar.DAY_OF_YEAR, rand.nextInt() % 45);
//			System.out.println(city[rand.nextInt(city.length)] + city[rand.nextInt(city.length)]);
//			System.out.println(t.queryLeftTickets(sdf.format(cal.getTime())
//					, city[rand.nextInt(city.length)], city[rand.nextInt(city.length)]));
//		}
//		System.out.println(t.queryLeftTickets("2016-04-30", "杭州", "西安"));
//		System.out.println(t.queryTrainInfo("Z56"));
//		System.out.println(t.queryPrice("T114", "西安", "杭州", "2016-05-10"));
		
//		System.out.println(t.queryLeftTickets("2016-04-30", "杭州", "北京"));
		HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 10);
		HttpContextBuilder contextBuilder = new HttpContextBuilder();
		contextBuilder.getDeployment().getActualResourceClasses().add(TrainInfoService.class);
		HttpContext context = contextBuilder.bind(httpServer);
		context.getAttributes().put("some.config.info", "42");
		httpServer.start();
	}
    /**
     * 得到某个日期车次对应的车次内部编码
     * @param queryDate
     * @param trainNoPub
     * @return null表示出错 NotFound表示未找到对应关系 其余正常输出
     */
	@GET
	@Path("/internalTrainNo")
	@Produces("text/plain")
    String getTrainNo(@QueryParam("queryDate") String queryDate
    		, @QueryParam("trainNoPub") String trainNoPub) {
    	DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date d = new Date();
		Date down = new Date();
		Date up = new Date();
		Calendar cl;
		try {
			cl = Calendar.getInstance();
			cl.add(Calendar.DAY_OF_YEAR, -1);
			down = cl.getTime();
			down = df.parse(df.format(down));
			cl = Calendar.getInstance();
			cl.add(Calendar.DAY_OF_YEAR, 60);
			up = cl.getTime();
			up = df.parse(df.format(up));
			d = df.parse(queryDate);
			if(!(d.before(up) && d.after(down))) {
				System.out.println("超出查询范围!");
				return null;
			}
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("时间格式转换错误！");
			return null;
		}
		
		if(Util.saveTrainList()) {
			String trainListRaw = "";
			try {
				FileInputStream fis = new FileInputStream(
						df.format(Calendar.getInstance().getTime()).toString());
				InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
				BufferedReader br = new BufferedReader(isr);
				String temp = "";
				while((temp = br.readLine()) != null) {
					trainListRaw += temp;
				}
				br.close();
			} catch(IOException e) {
				e.printStackTrace();
				System.out.println("文件内容读取出错！");
				return null;
			}
			if(trainListRaw.startsWith("var train_list ={")) {
				try {
					trainListRaw = trainListRaw.substring(
							trainListRaw.indexOf("{"), trainListRaw.lastIndexOf("}") + 1);
					int index = trainListRaw.indexOf(queryDate.toString()) + 1;
					int lastIndex = trainListRaw.indexOf("]}", index) + 2;
					System.out.println(queryDate);
					System.out.println(index);
					System.out.println(lastIndex);
//					System.out.println(trainListRaw.substring(0, index));
//					System.out.println(trainListRaw.substring(lastIndex));
					JSONObject trainListOneDay = JSONObject.fromObject(
							trainListRaw.substring(index, lastIndex));
					JSONArray trainListOneDayOnePart = new JSONArray();
					if(trainNoPub.matches("D[0-9]+")) {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("D");
					}
					else if(trainNoPub.matches("T[0-9]+")) {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("T");
					}
					else if(trainNoPub.matches("G[0-9]+")) {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("G");
					}
					else if(trainNoPub.matches("C[0-9]+")) {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("C");
					}
					else if(trainNoPub.matches("K[0-9]+")) {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("K");
					}
					else if(trainNoPub.matches("Z[0-9]+")) {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("Z");
					}
					else {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("O");
					}
					for(int i = 0; i < trainListOneDayOnePart.size(); i ++) {
						JSONObject jo = trainListOneDayOnePart.getJSONObject(i);
						if(jo.getString("station_train_code").startsWith(trainNoPub)) {
							return jo.getString("train_no");
						}
					}
					System.out.println("该车次不存在！");
					return "NotFound";
				} catch (Exception e) {
					System.out.println("车次映射文件出错！");
					return null;
				}
				
			}
			else {
				System.out.println("文件内容有误！");
				return null;
			}
		}
		else {
			return null;
		}
    }
	/**
	 * 获取内部站名
	 * @param stationNamePub
	 * @return null表示出错 NotFound表示未找到对应项 其余正常输出
	 */
	@GET
	@Path("/stationName")
	@Produces("text/plain")
	String getStationName(@QueryParam("stationNamePub") String stationNamePub) {
		if(Util.saveStationName()) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			String stationNameRaw = "";
			try {
				FileInputStream fis = new FileInputStream(
						"stationName_" + df.format(Calendar.getInstance().getTime()).toString());
				InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
				BufferedReader br = new BufferedReader(isr);
				String temp = "";
				while((temp = br.readLine()) != null) {
					stationNameRaw += temp;
				}
				br.close();
			} catch(IOException e) {
				e.printStackTrace();
				System.out.println("getStationName:文件内容读取出错！");
				return null;
			}
			if(stationNameRaw.startsWith("var station_names =")) {
				JSONObject stationName = new JSONObject();
				stationNameRaw = stationNameRaw.substring(
						stationNameRaw.indexOf("@") + 1, stationNameRaw.lastIndexOf("'"));
				String snr[] = stationNameRaw.split("@");
				for(String item : snr) {
					String key = item.split("\\|")[1];
					String value = item.split("\\|")[2];
					stationName.put(key, value);
				}
				if(stationName.containsKey(stationNamePub)) {
					return stationName.getString(stationNamePub);
				}
				else {
					return "NotFound";
				}
			}
			else {
				System.out.println("getStationName:获取stationName失败！");
				return null;
			}
		}
		else {
			return null;
		}
	}
	/**
	 * 获取公开车次名对应的始终区间
	 * @param trainNoPub
	 * @return null代表出错 NotFound代表未找到对应关系 其余正常返回
	 */
	@GET
	@Path("/internal")
	@Produces("text/plain")
	String getInterval(@QueryParam("trainNoPub") String trainNoPub) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, 10);
		String queryDate = df.format(c.getTime());
		if(Util.saveTrainList()) {
			String trainListRaw = "";
			try {
				FileInputStream fis = new FileInputStream(
						df.format(Calendar.getInstance().getTime()).toString());
				InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
				BufferedReader br = new BufferedReader(isr);
				String temp = "";
				while((temp = br.readLine()) != null) {
					trainListRaw += temp;
				}
				br.close();
			} catch(IOException e) {
				e.printStackTrace();
				System.out.println("getStartStationNo:文件内容读取出错！");
				return null;
			}
			if(trainListRaw.startsWith("var train_list ={")) {
				try {
					trainListRaw = trainListRaw.substring(trainListRaw.indexOf("{")
							, trainListRaw.lastIndexOf("}") + 1);
					int index = trainListRaw.indexOf(queryDate.toString()) + 12;
					int lastIndex = trainListRaw.indexOf("]}", index) + 2;
					JSONObject trainListOneDay = JSONObject.fromObject(
							trainListRaw.substring(index, lastIndex));
					JSONArray trainListOneDayOnePart = new JSONArray();
					if(trainNoPub.matches("D[0-9]+")) {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("D");
					}
					else if(trainNoPub.matches("T[0-9]+")) {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("T");
					}
					else if(trainNoPub.matches("G[0-9]+")) {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("G");
					}
					else if(trainNoPub.matches("C[0-9]+")) {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("C");
					}
					else if(trainNoPub.matches("K[0-9]+")) {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("K");
					}
					else if(trainNoPub.matches("Z[0-9]+")) {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("Z");
					}
					else {
						trainListOneDayOnePart = trainListOneDay.getJSONArray("O");
					}
					for(int i = 0; i < trainListOneDayOnePart.size(); i ++) {
						JSONObject jo = trainListOneDayOnePart.getJSONObject(i);
						if(jo.getString("station_train_code").startsWith(trainNoPub)) {
							return jo.getString("station_train_code")
									.substring(trainNoPub.length());
						}
					}
					System.out.println("getStartStationNo:该车次不存在！");
					return "NotFound";
				} catch (Exception e) {
					System.out.println("[车次：" + trainNoPub 
							+ " 日期：" + queryDate + "]车次映射文件读取错误！");
					return null;
				}
			}
			else {
				System.out.println("getStartStationNo:文件内容有误！");
				return null;
			}
		}
		else {
			return null;
		}
	}
	/**
	 * 返回参数车站在参数列车的第几站
	 * @param trainNoPub
	 * @param stationNamePub
	 * @return null代表出错 NotFound代表未找到对应关系 其余正常返回
	 */
	@GET
	@Path("/stationNo")
	@Produces("text/plain")
	String getStationNo(@QueryParam("trainNoPub") String trainNoPub
			, @QueryParam("stationNamePub") String stationNamePub) {
		JSONObject trainInfo = JSONObject.fromObject(queryTrainInfo(trainNoPub));
		if(trainInfo.containsKey("error")) {
			return null;
		}
		else {
			JSONArray data = trainInfo.getJSONObject("data").getJSONArray("data");
			for(int i = 0; i < data.size(); i ++) {
				if(data.getJSONObject(i).getString("station_name").equals(stationNamePub)) {
					return data.getJSONObject(i).getString("station_no");
				}
			}
			return "NotFound";
		}
	}
	/**
	 * 获取参数车次的座位类型
	 * @param trainNoPub
	 * @return null代表出错 NotFound代表未找到对应关系 其余正常返回
	 */
	@GET
	@Path("/seatTypes")
	@Produces("text/plain")
	String getSeatTypes(@QueryParam("trainNoPub") String trainNoPub) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, 10);
		String queryDate = df.format(c.getTime());
		String interval = getInterval(trainNoPub);
		if(interval == null) {
			return null;
		}
		else if(interval.equals("NotFound")) {
			return null;
		}
		else {
			String startStationPub = interval.substring(
					interval.indexOf("(") + 1, interval.indexOf('-'));
			String endStationPub = interval.substring(
					interval.indexOf("-") + 1, interval.lastIndexOf(")"));
			JSONObject leftTickets = JSONObject.fromObject(
					queryLeftTickets(queryDate, startStationPub, endStationPub));
			if(leftTickets.containsKey("error")) {
				return null;
			}
			else {
				JSONArray datas = leftTickets.getJSONObject("data")
						.getJSONArray("datas");
				for(int i = 0; i < datas.size(); i ++) {
					if(datas.getJSONObject(i).getString("station_train_code")
							.equals(trainNoPub)) {
						return datas.getJSONObject(i).getString("seat_types");
					}
				}
				return "NotFound";
			}
		}
	}
	/**
	 * 查询余票
	 * @param queryDate
	 * @param fromStationNamePub
	 * @param toStationNamePub
	 * @return JSON格式字符串 有error则表明出错
	 */
	@GET
	@Path("/leftTickets")
	@Produces("application/json")
	public String queryLeftTickets(@QueryParam("queryDate") String queryDate
			, @QueryParam("fromStationNamePub") String fromStationNamePub
			, @QueryParam("toStationNamePub") String toStationNamePub) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		try {
			queryDate = df.format(df.parse(queryDate));
		} catch (ParseException e) {
			e.printStackTrace();
			return "{\"error\":\"queryDate参数有误！\"}";
		}
		String fromStation = getStationName(fromStationNamePub);
		String toStation = getStationName(toStationNamePub);
		if(fromStation != null && toStation != null) {
			if(fromStation.equals("NotFound") || toStation.equals("NotFound")) {
				return "{\"error\":\"出发站点或到达站点错误！\"}";
			}
			else {
				String url = "https://kyfw.12306.cn/otn/lcxxcx/query?";
				url += "purpose_codes=ADULT";
				url += "&queryDate=" + queryDate;
				url += "&from_station=" + fromStation;
				url += "&to_station=" + toStation;
				return Util.sendHTTPGet(url);
			}
		}
		else {
			return "{\"error\":\"错误信息已打印！\"}";
		}
	}
	/**
	 * 查询车次信息
	 * @param trainNoPub
	 * @return JSON格式字符串 有error则表明出错
	 */
	@GET
	@Path("/trainInfo")
	@Produces("application/json")
	public String queryTrainInfo(@QueryParam("trainNoPub") String trainNoPub) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, 10);
		String queryDate = df.format(c.getTime());
		String trainNo = getTrainNo(queryDate, trainNoPub);
		String interval = getInterval(trainNoPub);
		if(interval == null) {
			return "{\"error\":\"错误信息已打印！\"}";
		}
		else if(interval.equals("NotFound")) {
			return "{\"error\":\"未找到该车次！\"}";
		}
		else {
			String startStationPub = interval.substring(
					interval.indexOf("(") + 1, interval.indexOf('-'));
			String endStationPub = interval.substring(
					interval.indexOf("-") + 1, interval.lastIndexOf(")"));
			String fromStation = getStationName(startStationPub);
			String toStation = getStationName(endStationPub);
			if(fromStation == null || toStation == null) {
				return "{\"error\":\"错误信息已打印！\"}";
			}
			else if(fromStation.equals("NotFound") || toStation.equals("NotFound")) {
				return "{\"error\":\"未找到该车次！\"}";
			}
			else {
				String url = "https://kyfw.12306.cn/otn/czxx/queryByTrainNo?";
				url += "train_no=" + trainNo;
				url += "&from_station_telecode=" + fromStation;
				url += "&to_station_telecode=" + toStation;
				url += "&depart_date=" + queryDate;
				return Util.sendHTTPGet(url);
			}
		}
	}
	/**
	 * 查询价格
	 * @param trainNoPub
	 * @param fromStationNamePub
	 * @param toStationNamePub
	 * @param queryDate
	 * @return JSON格式字符串 有error则表明出错
	 */
	@GET
	@Path("/price")
	@Produces("application/json")
	public String queryPrice(@QueryParam("trainNoPub") String trainNoPub
			, @QueryParam("fromStationNamePub") String fromStationNamePub
			, @QueryParam("queryDate") String toStationNamePub
			, @QueryParam("queryDate") String queryDate) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		try {
			queryDate = df.format(df.parse(queryDate));
		} catch (ParseException e) {
			e.printStackTrace();
			return "{\"error\":\"queryDate参数有误！\"}";
		}
		String trainNo = getTrainNo(queryDate, trainNoPub);
		String fromStationNum = getStationNo(trainNoPub, fromStationNamePub);
		String toStationNum = getStationNo(trainNoPub, toStationNamePub);
		String seatTypes = getSeatTypes(trainNoPub);
		if(seatTypes == null || fromStationNum == null || toStationNum == null || trainNo == null) {
			return "{\"error\":\"错误信息已打印！\"}";
		}
		else if(seatTypes.equals("NotFound") || fromStationNum.equals("NotFound") || toStationNum.equals("NotFound") || trainNo.equals("NotFound")) {
			return "{\"error\":\"未找到参数信息！\"}";
		}
		else {
			String url = "https://kyfw.12306.cn/otn/leftTicket/queryTicketPrice?";
			url += "train_no=" + trainNo;
			url += "&from_station_no=" + fromStationNum;
			url += "&to_station_no=" + toStationNum;
			url += "&seat_types=" + seatTypes;
			url += "&train_date=" + queryDate;
			return Util.sendHTTPGet(url);
		}
	}
}
