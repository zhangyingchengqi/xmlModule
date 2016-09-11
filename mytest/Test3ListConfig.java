import bean.ListConfig;
import bean.SimpleConfig;

import com.yc.ycframework.module.xmlModule.XmlConverter;
import com.yc.ycframework.module.xmlModule.XmlConverterImpl;


public class Test3ListConfig {

	public static void main(String[] args) {
		XmlConverter converter=new XmlConverterImpl();
		converter.setXml("list.xml");
		converter.setTargetClass("bean.ListConfig");
		ListConfig sc=converter.getTargetObject();
		System.out.println(  sc.getLevels().size());
	
	}

}
