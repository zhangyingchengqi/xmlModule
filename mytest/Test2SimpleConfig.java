import bean.SimpleConfig;

import com.yc.ycframework.module.xmlModule.XmlConverter;
import com.yc.ycframework.module.xmlModule.XmlConverterImpl;


public class Test2SimpleConfig {

	public static void main(String[] args) {
		XmlConverter converter=new XmlConverterImpl();
		converter.setXml("simple.xml");
		converter.setTargetClass("bean.SimpleConfig");
		SimpleConfig sc=converter.getTargetObject();
		System.out.println(   sc.getBVal()+"\t"+sc.getCVal());
	}

}
