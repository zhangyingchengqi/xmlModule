package com.yc.ycframework.module.xmlModule;

/**
 * Xmlת�����߽ӿ�
 * @author zy
 *
 */
public interface XmlConverter {
	/**
	 * ����Ҫת����xml,Ҳ�������ļ���
	 * @param xml: ������xml���ַ�����xml�ļ�����Ҳ�����Ǿ���·����Ҳ���������·��
	 */
	public void setXml(String xml);
	
	public String getXml();
	
	/**
	 * ����Ҫת����Ŀ����
	 * @param targetClass: ת��Ŀ��������
	 */
	public void setTargetClass(String targetClass);
	
	public String getTargetClass();
	
	/**
	 * ��xmlת��Ϊjava���� 
	 */
	public void xml2Java();
	/**
	 * ��ȡת����Ķ���ʵ��
	 * @param <T>
	 * @return
	 */
	public <T> T getTargetObject();
}
