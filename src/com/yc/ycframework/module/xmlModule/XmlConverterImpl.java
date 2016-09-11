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
	/** 是否转换完毕 */
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
	 * 设置要转换的目标类
	 * @param: 转换目标的类对象
	 */
	@Override
	public void setTargetClass(String targetClass) {
		this.targetClassName=targetClass;
		converted=false;
	}

	/**
	 * 设置要转换的xml,也可以是xml文件名
	 */
	@Override
	public void setXml(String xml) {
		this.xml=xml;
		converted=false;
	}

	@Override
	public void xml2Java() {
		//判断是否已经转换完成
		if ( converted ){
			return;
		}
		try{
		//解析xml或xmlFile 成  dom
		Node rootNode=getDomRoot(xml);
		//创建映射的目标对象
		Object obj=ClassReflector.newInstance( targetClassName);
		//遍历dom,取出其中的元素，设置到对象对应的属性上
		setObjProperties(obj, rootNode );
		//保存构建好的对象
		targetObject=obj;
		converted=true;
		}catch( Exception e){
			throw new RuntimeException( e );
		}
	}

	/**
	 * 将xml字符串或文件，解析成dom,并返回根节点
	 * @param xml:  xml即可以是一个xml文本，也可以是一个xml文件名，如果是文件名，则需要从相对路径，绝对路径，类路径三个位置尝试加载
	 * @return  Node:   xml文件的根节点
	 * @throws Exception
	 */
	private Node getDomRoot(String xml) throws Exception{
		if(xml==null  ||  xml.length()<1){
			throw new Exception("xml param is null");
		}
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		DocumentBuilder builder=factory.newDocumentBuilder();
		Document doc=null;
		//判断  xml是否为xml字符串
		if(xml.charAt(0)=='<'){
			ByteArrayInputStream bais=new ByteArrayInputStream( xml.getBytes()  );
			doc=builder.parse(  bais  );
		}else{   //不是xml字符串，则表示是一个xml文件路径
			try{
				doc=builder.parse( xml  );
			}catch(  FileNotFoundException e ){
				//如果找不到这个文件,则尝试从类路径再找
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
	 * 设置xml的对象属性: 四种类型的对象属性:  a. 简单类型   b. 对象类型   c. List    d. map
	 * @param obj
	 * @param node
	 * @throws Exception
	 */
	private void setObjProperties(Object obj, Node node)throws Exception{
		ClassReflector reflector=new ClassReflector(obj);
		
		//取出当前节点的子元素
		NodeList list=node.getChildNodes();
		for( int i=0;i<list.getLength();i++){
			Node childNode=list.item(i);
			//判断是否为一个元素节点,只需要操作元素节点,非元素节点则跳过
			if(childNode.getNodeType()!=Node.ELEMENT_NODE){
				continue;
			}
			//获取这个节点的节点名,并且这个节点名就是对象的属性名
			String propertyName=childNode.getNodeName();
			//获取这个节点下的子节点,如果子节点数为0，则表示是一个普通属性
			NodeList grandChildList=childNode.getChildNodes();
			//子元素节点数
			int childElementNodeCount=getElementNodeCount(  grandChildList  );
			if(childElementNodeCount==0){
				//是基本元素节点
				String nodeValue=getNodeValue(  grandChildList );
				reflector.setPropertyValue(  propertyName,   nodeValue ,   DataType.DT_String);
			}else{
				//表示是复杂属性，需要判断   是list, map, 还是用户自定义的类型.
				//获取属性类型
				String dataTypeName=reflector.getPropertyType(  propertyName);
				int propertyDataType=DataType.getDataType( dataTypeName);
				//区分属性是list, map,还是对象
				switch(    propertyDataType ){
				case DataType.DT_UserDefine:   //对象类型
					//创建一个属性对象
					Object propObject=ClassReflector.newInstance( dataTypeName);
					//解析节点，设置到属性对象上
					setObjProperties(propObject, childNode);
					//设置到属性上
					reflector.setPropertyValue( propertyName, propObject);
					break;
				case DataType.DT_List:
				//case DataType.DT_LinkedList:
				//case DataType.DT_ArrayList:
					//获取list属性值，如果当前值为null, 则会创建一个list返回，并将needSetList设置为true
					DataWrapper<Boolean> needSetList=new DataWrapper<Boolean>(false);
					List propList=getListProperty(reflector, propertyName, propertyDataType, needSetList);
					//解析节点，将子元素添加到list中
					fillList(propList, childNode, dataTypeName);
					//将list设置进去
					if(needSetList.getVal() ){
						reflector.setPropertyValue(propertyName,propList);
					}
					break;
				case DataType.DT_Map:
				//case DataType.DT_HashMap:
					//获取map的属性的值，如果当前值为null,则会创建一个map返回，并将needSetMap设置为true
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
	 * 获取类型为List的属性值，如果值为 null, 则创建一个list, 设置到相应的属性上
	 * @param reflector
	 * @param propertyName
	 * @param propertyDataType
	 * @param needSetValue
	 * @return
	 */
	private List getListProperty( ClassReflector reflector, String propertyName,  int propertyDataType, DataWrapper<Boolean> needSetValue){
		//先调用get方法获取属性值
		List propList=(List)reflector.getPropertyValue(propertyName);
		//如果属性值为null，则创建一个
		if( propList==null){
			needSetValue.setVal(true);
			String typeName=reflector.getPropertyType(propertyName);
			propList=(List)ClassReflector.newInstance(typeName);
		}
		return propList;
	}
	
	private void fillList(List arrayList,  Node childNode, String dataTypeName)throws Exception{
		//获取list中单个元素的数据类型
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
			//遍历各子节点，将每个子元素转换成相应的类型，放入list中
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
			//用户自定义的类型
			NodeList grandChildList=childNode.getChildNodes();
			for(  int j=0;j<grandChildList.getLength();j++){
				Node grandChildNode=grandChildList.item(j);
				if( grandChildNode.getNodeType()!=Node.ELEMENT_NODE){
					continue;
				}
				//创建一个对象的实例
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
		//获取key的类型
		String keyDataTypeName=DataType.getElementTypeName(dataTypeName,0);
		if( keyDataTypeName.equals("java.lang.Object")){
			keyDataTypeName="java.lang.String";
		}
		int keyDataType=DataType.getDataType(keyDataTypeName);
		boolean bKeySimpleType=DataType.isSimpleType(  keyDataType );
		
		//获取value的类型
		String valueDataTypeName=DataType.getElementTypeName(dataTypeName, 1);
		if(  valueDataTypeName.equals("java.lang.Object")){
			valueDataTypeName="java.lang.String";
		}
		int valueDataType=DataType.getDataType(valueDataTypeName);
		boolean bValueSimpleType=DataType.isSimpleType(valueDataType);
		
		//取子节点,剔除非element的子节点
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
			
			//创建key对象
			Object keyObject=null;
			if(bKeySimpleType){
				keyObject=DataType.toType(getNodeValue(keyNode), DataType.DT_String,keyDataType);
			}else{
				keyObject=ClassReflector.newInstance(keyDataTypeName);
				setObjProperties(keyObject, keyNode);
			}
			//创建value对象
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
















