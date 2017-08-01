import static com.esotericsoftware.kryo.util.GenericsUtil.*;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationCompatTestData.TestData;
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.minlog.Log;
import com.esotericsoftware.minlog.Log.Logger;

public class TestKryo {

	static public class Test1<FLOAT, STRING, OBJECT> {
		public ArrayList<String> fromField1;
		public ArrayList<FLOAT> fromSubClass;
		public ArrayList<STRING> fromSubSubClass;
		public HashMap<FLOAT, STRING> multipleFromSubClasses;
		public HashMap<FLOAT, OBJECT> multipleWithUnknown;
		public FLOAT[] arrayFromSubClass;
		public STRING[] arrayFromSubSubClass;
	}

	static public class Test2<STRING, OBJECT> extends Test1<Float, STRING, OBJECT> {
		public STRING known;
		public STRING[] array1;
		public STRING[][] array2;
		public ArrayList<OBJECT> unknown1;
		public OBJECT[] arrayUnknown1;
		public ArrayList<String> fromField2;
		public ArrayList<STRING>[] arrayWithTypeVar;
		public ArrayList<ArrayList<String>> fromFieldNested;
	}

	static public class Test3<DOUBLE extends Number & Comparable, OBJECT> extends Test2<String, OBJECT> {
		public ArrayList<String> fromField3;
		public ArrayList raw;
		public OBJECT unknown2;
		public OBJECT[] arrayUnknown2;
		public ArrayList<?> unboundWildcard;
		public ArrayList<? extends Number> upperBound;
		public ArrayList<? super Integer> lowerBound;
		public ArrayList<DOUBLE> multipleUpperBounds;
	}

	static public class Test4<OBJECT> extends Test3<Double, OBJECT> {
	}

	static public void main (String[] args) throws Exception {
		String[] names = { //
			"ArrayList<String> fromField1", //
			"ArrayList<FLOAT> fromSubClass", //
			"ArrayList<STRING> fromSubSubClass", //
			"HashMap<FLOAT, STRING> multipleFromSubClasses", //
			"HashMap<FLOAT, OBJECT> multipleWithUnknown", //
			"FLOAT[] arrayFromSubClass", //
			"STRING[] arrayFromSubSubClass", //

			"STRING known", //
			"STRING[] array1", //
			"STRING[][] array2", //
			"ArrayList<OBJECT> unknown1", //
			"OBJECT[] arrayUnknown1", //
			"ArrayList<String> fromField2", //
			"ArrayList<STRING>[] arrayWithTypeVar", //
			"ArrayList<ArrayList> fromFieldNested", //

			"ArrayList<String> fromField3", //
			"ArrayList raw", //
			"OBJECT unknown2", //
			"OBJECT[] arrayUnknown2", //
			"ArrayList<OBJECT> unboundWildcard", //
			"ArrayList<NUMBER> upperBound", //
			"ArrayList<INTEGER> lowerBound", //

			"ArrayList<DOUBLE> multipleUpperBounds", //
		};
		// names = new String[] {"ArrayList<ArrayList<String>> fromFieldNested"};
//		for (String value1 : names) {
//			int index = value1.lastIndexOf(' ');
//			String type = value1.substring(0, index), name = value1.substring(index + 1);
//
//			Field field = Test4.class.getField(name);
//			Class declaringClass = field.getDeclaringClass();
//			Class serializingClass = Test4.class;
//			Type genericType = field.getGenericType();
//
//			Class fieldClass = resolveType(declaringClass, serializingClass, genericType);
//			if (fieldClass == null) fieldClass = field.getType();
//
//			Class[] generics = resolveTypeParameters(declaringClass, serializingClass, genericType);
//
//			String value2 = fieldClass.getSimpleName().replaceAll("[\\[\\]]", "");
//			if (generics != null) {
//				value2 += "<";
//				for (int i = 0, n = generics.length; i < n; i++) {
//					if (i > 0) value2 += ", ";
//					value2 += generics[i].getSimpleName();
//				}
//				value2 += ">";
//			}
//			value2 += fieldClass.getSimpleName().replaceAll("[^\\[\\]]", "");
//			value2 += " " + name;
//
//			if (!value1.equalsIgnoreCase(value2)) throw new RuntimeException();
//		}

// Field field = Test.class.getField("a");
// Class type = field.getType();
//
// System.out.println("TypeParameters:");
// for (TypeVariable p : type.getTypeParameters())
// System.out.println("\t" + p);
//
// System.out.println("GenericString: " + type.toGenericString());
//
// Type genericType = field.getGenericType();
// System.out.println("GenericType: " + genericType.getClass().getSimpleName());
// if (genericType instanceof ParameterizedType) {
// ParameterizedType p = (ParameterizedType)genericType;
//
// System.out.println("ActualTypeArguments:");
// for (Type arg : p.getActualTypeArguments())
// System.out.println("\t" + arg);
//
// }
//
		if (true) return;

		Log.TRACE();
		Log.setLogger(new Logger() {
			public void log (int level, String category, String message, Throwable ex) {
				System.out.println(message);
				if (ex != null) ex.printStackTrace();
			}
		});

// ListContainer container1 = new ListContainer();
// container1.list = new ArrayList();
// ArrayList one = new ArrayList();
// one.add("one");
// container1.list.add(one);
// ArrayList two = new ArrayList();
// two.add("two");
// container1.list.add(two);
// container1.list.add("one");
// container1.list.add("two");
// container1.list.add(new ArrayList());
// container1.list.add(new ArrayList());
// container1.list.add(new ArrayList());
// container1.list.add(new ArrayList());
// container1.list.add(new ArrayList());
// container1.list.add(new ArrayList());
// container1.list.add(new ArrayList());

// Test test1 = new Test();
// test1.container = container1;
//
// Test test2 = roundTrip(test1, true);
// System.out.println(test1.container.list);
// System.out.println(test2.container.list);

// ListContainer container2 = roundTrip(container1, true);
// System.out.println(container1.list);
// System.out.println(container2.list);

		System.out.println();

// roundTrip(new TestData(), true);
// test2 = roundTrip(test1, false);
// System.out.println(test1.container.list);
// System.out.println(test2.container.list);

// container2 = roundTrip(container1, false);
// System.out.println(container1.list);
// System.out.println(container2.list);
	}

	static <T> T roundTrip (T object, boolean optimizeGenerics) {
		Kryo kryo = new Kryo();

		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setOptimizedGenerics(optimizeGenerics);
		kryo.setDefaultSerializer(factory);

// kryo.register(ListContainer.class);
// kryo.register(Test.class);
		kryo.register(ArrayList.class);
		kryo.setReferences(false);

		if (object instanceof TestData) {
			kryo.register(TestData.class);
			kryo.setRegistrationRequired(false);
			kryo.setReferences(true);
			kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		}

		Output output = new Output(1024 * 10);
		kryo.writeObject(output, object);
		output.flush();

		Input input = new Input(output.getBuffer(), 0, output.position());
		Object object2 = kryo.readObject(input, object.getClass());

		System.out.println("Size: " + output.position());
		return (T)object2;
	}
}
