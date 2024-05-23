/**
 * 
 */
package ssrd.hibernate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * hibernate5用来获取Metadata的工具类<br>
 * 需要通过注册服务的方式获得回调机会进而获取Metadata，具体方法如下。<br>
 * 1. 创建如下路径的文件
 * META-INF/services/org.hibernate.boot.spi.SessionFactoryBuilderFactory
 * 2. 在以上文件中写入完整工具类名：com.aj.frame.db.hibernate.HibernateEntityMetaDatas
 *  * @author rechard
 *
 */
public class HibernateEntityMetaDatas implements SessionFactoryBuilderFactory {
	private final static Set<Metadata> metadatas = new HashSet<Metadata>();
	/**
	 * 静态缓存(Map<映射类名,PersistentClass>)
	 */
	private final static Map<String, Map<String, PersistentClass>> persistentClasses = new HashMap<String, Map<String, PersistentClass>>();
	/**
	 * 根据模型catalog和映射类名获取对应的PersistentClass对象
	 * @param catalog
	 * @param className
	 * @return
	 */
	public static PersistentClass getPersistentClass(String catalog, String className) {
		synchronized (HibernateEntityMetaDatas.persistentClasses) {
			Map<String, PersistentClass> pcs = HibernateEntityMetaDatas.persistentClasses.get(catalog);
			if (pcs != null)
				return pcs.get(className);
		}
		return null;
	}
	public static Iterator<Column> getColumnIterator(Property property) {
//		return (Iterator<Column>)property.getColumnIterator(); // hibernate5代码
		return property.getColumns().iterator(); // hibernate6代码
	}
	public static Iterator<Property> getPropertyClosureIterator(PersistentClass pc) {
//		return (Iterator<Property>)pc.getPropertyIterator(); // hibernate5代码
		return pc.getProperties().iterator(); // hibernate6代码
	}
	public static Column getColumn(PersistentClass pc, String propertyName) {
		Property property = null;
		try {
			property = pc.getProperty(propertyName);
		} catch (Exception e) {}
		if (property != null) {
			Iterator<Column> it = getColumnIterator(property);
			if (it != null && it.hasNext())
				return it.next();
		}
		KeyValue identifier = pc.getIdentifier();
		if (identifier != null && identifier instanceof Component) {
			List<Property> ps = ((Component)identifier).getProperties();
			if (ps != null && ps.size() > 1) {
				// 组合主键的情况
				Iterator<Property> pit = ps.iterator();
				while (pit.hasNext()) {
					property = pit.next();
					if (property.getName().equals(propertyName)) {
						Iterator<Column> it = getColumnIterator(property);
						if (it != null && it.hasNext())
							return it.next();
					}
				}
			}
		}
		return null;
	}
	public static Column getIdentifierColumn(PersistentClass pc) {
		Property property = null;
		try {
			property = pc.getIdentifierProperty();
		} catch (Exception e) {}
		if (property != null) {
			Iterator<Column> it = getColumnIterator(property);
			if (it != null && it.hasNext())
				return it.next();
		}
		return null;
	}
	public static Property getProperty(PersistentClass pc, String columnName) {
		Iterator<Property> pit = getPropertyClosureIterator(pc);
		if (pit != null) {
			while (pit.hasNext()) {
				Property property = pit.next();
				Iterator<Column> it = getColumnIterator(property);
				if (it != null) {
					while (it.hasNext()) {
						Column column = it.next();
						if (column.getName().equalsIgnoreCase(columnName))
							return property;
					}
				}
			}
		}
		KeyValue identifier = pc.getIdentifier();
		if (identifier != null && identifier instanceof Component) {
			List<Property> ps = ((Component)identifier).getProperties();
			if (ps != null && ps.size() > 1) {
				// 组合主键的情况
				pit = ps.iterator();
				while (pit.hasNext()) {
					Property property = pit.next();
					Iterator<Column> it = getColumnIterator(property);
					if (it != null) {
						while (it.hasNext()) {
							Column column = it.next();
							if (column.getName().equalsIgnoreCase(columnName))
								return property;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * 
	 */
	public HibernateEntityMetaDatas() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder(MetadataImplementor metadata,
			SessionFactoryBuilderImplementor defaultBuilder) {
		synchronized (HibernateEntityMetaDatas.metadatas) {
			HibernateEntityMetaDatas.metadatas.add(metadata);
		}
		synchronized (HibernateEntityMetaDatas.persistentClasses) {
			if (metadata.getEntityBindings() != null) {
				Iterator<PersistentClass> it = metadata.getEntityBindings().iterator();
				while (it.hasNext()) {
					PersistentClass pc = it.next();
					String catalog = pc.getTable().getCatalog();
					if (catalog == null)
						catalog = "";
					Map<String, PersistentClass> pcmap = HibernateEntityMetaDatas.persistentClasses.get(catalog);
					if (pcmap == null) {
						pcmap = new HashMap<String, PersistentClass>();
						HibernateEntityMetaDatas.persistentClasses.put(catalog, pcmap);
					}
					pcmap.put(pc.getMappedClass().getName(), pc);
				}
			}
		}
		return defaultBuilder;
	}
}
