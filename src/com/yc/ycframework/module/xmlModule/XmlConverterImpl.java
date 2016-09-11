package com.yc.ycframework.module.xmlModule;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.yc.ycframework.module.util.ClassReflector;
import com.yc.ycframework.module.util.DataType;
import com.yc.ycframework.module.util.DataWrapper;

public class XmlConverterImpl implements XmlConverter {
	/** �Ƿ�ת����� */
	private boolean converted=false;
	private String xml;
	private String targetClassName;
	private Object targetObject;

	@Override
	public String getTargetClass() {
		return targetClassName;
	}

	@Override
	public <T> T getTargetObject() {
		if(!converted){
			xml2Java();
		}
		return (T)targetObject;
	}

	@Override
	public String getXml() {
		return xml;
	}

	/**
	 * ����Ҫת����Ŀ����
	 * @param: ת��Ŀ��������
	 */
	@Override
	public void setTargetClass(String targetClass) {
		this.targetClassName=targetClass;
		converted=false;
	}

	/**
	 * ����Ҫת����xml,Ҳ������xml�ļ���
	 */
	@Override
	public void setXml(String xml) {
		this.xml=xml;
		converted=false;
	}

	@Override
	public void xml2Java() {
		//�ж��Ƿ��Ѿ�ת�����
		if ( converted ){
			return;
		}
		try{
		//����xml��xmlFile ��  dom
		Node rootNode=getDomRoot(xml);
		//����ӳ���Ŀ�����
		Object obj=ClassReflector.newInstance( targetClassName);
		//����dom,ȡ�����е�Ԫ�أ����õ������Ӧ��������
		setObjProperties(obj, rootNode );
		//���湹���õĶ���
		targetObject=obj;
		converted=true;
		}catch( Exception e){
			throw new RuntimeException( e );
		}
	}

	/**
	 * ��xml�ַ������ļ���������dom,�����ظ��ڵ�
	 * @param xml:  xml��������һ��xml�ı���Ҳ������һ��xml�ļ�����������ļ���������Ҫ�����·��������·������·������λ�ó��Լ���
	 * @return  Node:   xml�ļ��ĸ��ڵ�
	 * @throws Exception
	 */
	private Node getDomRoot(String xml) throws Exception{
		if(xml==null  ||  xml.length()<1){
			throw new Exception("xml param is null");
		}
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		DocumentBuilder builder=factory.newDocumentBuilder();
		Document doc=null;
		//�ж�  xml�Ƿ�Ϊxml�ַ���
		if(xml.charAt(0)=='<'){
			ByteArrayInputStream bais=new ByteArrayInputStream( xml.getBytes()  );
			doc=builder.parse(  bais  );
		}else{   //����xml�ַ��������ʾ��һ��xml�ļ�·��
			try{
				doc=builder.parse( xml  );
			}catch(  FileNotFoundException e ){
				//����Ҳ�������ļ�,���Դ���·������
				URL url=Thread.currentThread().getContextClassLoader().getResource(xml);
				if(url==null){
					throw e;
				}else{
					doc=builder.parse(  url.toString() );
				}
			}
		}
		doc.normalize();
		Node docNode=doc.getDocumentElement();
		return docNode;
	}
	
	/**
	 * ����xml�Ķ�������: �������͵Ķ�������:  a. ������   b. ��������   c. List    d. map
	 * @param obj
	 * @param node
	 * @throws Exception
	 */
	private void setObjProperties(Object obj, Node node)throws Exception{
		ClassReflector reflector=new ClassReflector(obj);
		
		//ȡ����ǰ�ڵ����Ԫ��
		NodeList list=node.getChildNodes();
		for( int i=0;i<list.getLength();i++){
			Node childNode=list.item(i);
			//�ж��Ƿ�Ϊһ��Ԫ�ؽڵ�,ֻ��Ҫ����Ԫ�ؽڵ�,��Ԫ�ؽڵ�������
			if(childNode.getNodeType()!=Node.ELEMENT_NODE){
				continue;
			}
			//��ȡ����ڵ�Ľڵ���,��������ڵ������Ƕ����������
			String propertyName=childNode.getNodeName();
			//��ȡ����ڵ��µ��ӽڵ�,����ӽڵ���Ϊ0�����ʾ��һ����ͨ����
			NodeList grandChildList=childNode.getChildNodes();
			//��Ԫ�ؽڵ���
			int childElementNodeCount=getElementNodeCount(  grandChildList  );
			if(childElementNodeCount==0){
				//�ǻ���Ԫ�ؽڵ�
				String nodeValue=getNodeValue(  grandChildList );
				reflector.setPropertyValue(  propertyName,   nodeValue ,   DataType.DT_String);
			}else{
				//��ʾ�Ǹ������ԣ���Ҫ�ж�   ��list, map, �����û��Զ��������.
				//��ȡ��������
				String dataTypeName=reflector.getPropertyType(  propertyName);
				int propertyDataType=DataType.getDataType( dataTypeName);
				//����������list, map,���Ƕ���
				switch(    propertyDataType ){
				case DataType.DT_UserDefine:   //��������
					//����һ�����Զ���
					Object propObject=ClassReflector.newInstance( dataTypeName);
					//�����ڵ㣬���õ����Զ�����
					setObjProperties(propObject, childNode);
					//���õ�������
					reflector.setPropertyValue( propertyName, propObject);
					break;
				case DataType.DT_List:
				//case DataType.DT_LinkedList:
				//case DataType.DT_ArrayList:
					//��ȡlist����ֵ�������ǰֵΪnull, ��ᴴ��һ��list���أ�����needSetList����Ϊtrue
					DataWrapper<Boolean> needSetList=new DataWrapper<Boolean>(false);
					List propList=getListProperty(reflector, propertyName, propertyDataType, needSetList);
					//�����ڵ㣬����Ԫ����ӵ�list��
					fillList(propList, childNode, dataTypeName);
					//��list���ý�ȥ
					if(needSetList.getVal() ){
						reflector.setPropertyValue(propertyName,propList);
					}
					break;
				case DataType.DT_Map:
				//case DataType.DT_HashMap:
					//��ȡmap�����Ե�ֵ�������ǰֵΪnull,��ᴴ��һ��map���أ�����needSetMap����Ϊtrue
					DataWrapper<Boolean> needSetMap=new DataWrapper<Boolean>(false);
					Map propMap=getMapProperty(reflector, propertyName, needSetMap);
					fillMap(propMap, childNode, dataTypeName);
					if( needSetMap.getVal() ){
						reflector.setPropertyValue( propertyName, propMap);
					}
					break;
					default:
						throw new Exception(propertyName+"is illegal");
				}
			}
		}
	}
	/**
	 * ��ȡ����ΪList������ֵ�����ֵΪ null, �򴴽�һ��list, ���õ���Ӧ��������
	 * @param reflector
	 * @param propertyName
	 * @param propertyDataType
	 * @param needSetValue
	 * @return
	 */
	private List getListProperty( ClassReflector reflector, String propertyName,  int propertyDataType, DataWrapper<Boolean> needSetValue){
		//�ȵ���get������ȡ����ֵ
		List propList=(List)reflector.getPropertyValue(propertyName);
		//�������ֵΪnull���򴴽�һ��
		if( propList==null){
			needSetValue.setVal(true);
			String typeName=reflector.getPropertyType(propertyName);
			propList=(List)ClassReflector.newInstance(typeName);
		}
		return propList;
	}
	
	private void fillList(List arrayList,  Node childNode, String dataTypeName)throws Exception{
		//��ȡlist�е���Ԫ�ص���������
		String itemDataTypeName=DataType.getElementTypeName(dataTypeName);
		int itemDataType=DataType.getDataType(itemDataTypeName);
		switch( itemDataType){
		case DataType.DT_Byte:
		case DataType.DT_Short:
		case DataType.DT_Integer:
		case DataType.DT_Long:
		case DataType.DT_Float:
		case DataType.DT_Double:
		case DataType.DT_Character:
		case DataType.DT_String:
		case DataType.DT_Boolean:
		case DataType.DT_Date:
		{
			//�������ӽڵ㣬��ÿ����Ԫ��ת������Ӧ�����ͣ�����list��
			NodeList grandChildList=childNode.getChildNodes();
			for(int j=0;j<grandChildList.getLength();j++){
				Node grandChildNode=grandChildList.item(j);
				if( grandChildNode.getNodeType()!=Node.ELEMENT_NODE){
					continue;
				}
				Object itemValue=DataType.toType(getNodeValue(grandChildNode), DataType.DT_String,itemDataType);
				arrayList.add(itemValue);
			}
		}
			break;
		case DataType.DT_UserDefine:
		{
			//�û��Զ��������
			NodeList grandChildList=childNode.getChildNodes();
			for(  int j=0;j<grandChildList.getLength();j++){
				Node grandChildNode=grandChildList.item(j);
				if( grandChildNode.getNodeType()!=Node.ELEMENT_NODE){
					continue;
				}
				//����һ�������ʵ��
				Object item=ClassReflector.newInstance(itemDataTypeName);
				arrayList.add(item);
				setObjProperties(item, grandChildNode);
			}
		}
		break;
		default:
			throw new RuntimeException( childNode.getNodeName()+"is illegal...");
			
		}
		
		
	
	}
	
	private void fillMap(Map propMap, Node childNode, String dataTypeName)throws Exception{
		//��ȡkey������
		String keyDataTypeName=DataType.getElementTypeName(dataTypeName,0);
		if( keyDataTypeName.equals("java.lang.Object")){
			keyDataTypeName="java.lang.String";
		}
		int keyDataType=DataType.getDataType(keyDataTypeName);
		boolean bKeySimpleType=DataType.isSimpleType(  keyDataType );
		
		//��ȡvalue������
		String valueDataTypeName=DataType.getElementTypeName(dataTypeName, 1);
		if(  valueDataTypeName.equals("java.lang.Object")){
			valueDataTypeName="java.lang.String";
		}
		int valueDataType=DataType.getDataType(valueDataTypeName);
		boolean bValueSimpleType=DataType.isSimpleType(valueDataType);
		
		//ȡ�ӽڵ�,�޳���element���ӽڵ�
		LinkedList<Node> itemList=new LinkedList<Node>();
		NodeList grandChildList=childNode.getChildNodes();
		for(int j=0;j<grandChildList.getLength();j++){
			Node grandChildNode=grandChildList.item(j);
			if( grandChildNode.getNodeType()==Node.ELEMENT_NODE){
				itemList.add(grandChildNode);
			}
		}
		grandChildList=null;
		int itemCount=itemList.size()/2;
		int index=0;
		for(int i=0;i<itemCount;i++){
			Node keyNode=itemList.get(index++);
			Node valueNode=itemList.get(index++);
			
			//����key����
			Object keyObject=null;
			if(bKeySimpleType){
				keyObject=DataType.toType(getNodeValue(keyNode), DataType.DT_String,keyDataType);
			}else{
				keyObject=ClassReflector.newInstance(keyDataTypeName);
				setObjProperties(keyObject, keyNode);
			}
			//����value����
			Object valueObject=null;
			if(bValueSimpleType){
				valueObject=DataType.toType(getNodeValue(valueNode),DataType.DT_String,valueDataType);
			}else{
				valueObject=ClassReflector.newInstance(valueDataTypeName);
				setObjProperties( valueObject,valueNode);
			}
			propMap.put( keyObject, valueObject);
		}
	}
	
	private Map getMapProperty( ClassReflector reflector, String propertyName, DataWrapper<Boolean> needSetMap ){
		Map propMap=(Map)reflector.getPropertyValue(propertyName);
		if( propMap==null){
			propMap=new HashMap();
			needSetMap.setVal(true);
		}
		return propMap;
	}
	
	private int getElementNodeCount( NodeList nodeList){
		int elementCount=0;
		int iLen=nodeList.getLength();
		for(int i=0;i<iLen;i++){
			Node n=nodeList.item(i);
			if( n.getNodeType()==Node.ELEMENT_NODE){
				elementCount++;
			}
		}
		return elementCount;
	}
	
	private String getNodeValue( Node node){
		NodeList list=node.getChildNodes();
		return getNodeValue(list);
	}
	
	private String getNodeValue(NodeList nodeList){
		int iLen=nodeList.getLength();
		for(int i=0;i<iLen;i++){
			Node n=nodeList.item(i);
			if( n.getNodeType()==Node.TEXT_NODE){
				return n.getNodeValue();
			}
		}
		return null;
	}
}
















