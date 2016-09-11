package com.yc.ycframework.module.xmlModule;

/**
 * Xml转换工具接口
 * @author zy
 *
 */
public interface XmlConverter {
	/**
	 * 设置要转换的xml,也可以是文件名
	 * @param xml: 可以是xml的字符串，xml文件名，也可以是绝对路径，也可以是相对路径
	 */
	public void setXml(String xml);
	
	public String getXml();
	
	/**
	 * 设置要转换的目标类
	 * @param targetClass: 转换目标的类对象
	 */
	public void setTargetClass(String targetClass);
	
	public String getTargetClass();
	
	/**
	 * 将xml转换为java对象。 
	 */
	public void xml2Java();
	/**
	 * 获取转换后的对象实例
	 * @param <T>
	 * @return
	 */
	public <T> T getTargetObject();
}
