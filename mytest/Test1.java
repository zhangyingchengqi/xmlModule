import bean.ServerInfo;

import com.yc.ycframework.module.xmlModule.XmlConverter;
import com.yc.ycframework.module.xmlModule.XmlConverterImpl;

/**
 * �yԇ���ε�xml�ļ����xȡ,  xml��һ���ַ���.
 * @author Administrator
 *
 */
public class Test1 {

	public static void main(String[] args) {
		XmlConverter converter=new XmlConverterImpl();
		String xml="<ServerInfo><serverIp>192.168.0.2</serverIp><serverPort>90</serverPort></ServerInfo>";
		converter.setXml(xml);
		converter.setTargetClass(    "bean.ServerInfo");
		ServerInfo si=converter.getTargetObject();
		System.out.println(   si.getServerIp()+"\t"+si.getServerPort());
	}

}
