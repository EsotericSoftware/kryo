import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.minlog.Log;
import com.esotericsoftware.minlog.Log.Logger;

public class TestKryo {

	static public class ListContainer<T> {
		public ArrayList<T> list;
	}

	static public class Test {
		public ListContainer<String> container;
	}

	static public void main (String[] args) throws Exception {

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
// if (true) return;

		Log.TRACE();
		Log.setLogger(new Logger() {
			public void log (int level, String category, String message, Throwable ex) {
				System.out.println(message);
				if (ex != null) ex.printStackTrace();
			}
		});

		ListContainer<String> container1 = new ListContainer();
		container1.list = new ArrayList();
		container1.list.add("one");
		container1.list.add("two");

		Test test1 = new Test();
		test1.container = container1;

		Test test2 = roundTrip(test1);
		System.out.println(test1.container.list);
		System.out.println(test2.container.list);

		ListContainer container2 = roundTrip(container1);
		System.out.println(container1.list);
		System.out.println(container2.list);
	}

	static <T> T roundTrip (T object) {
		Kryo kryo = new Kryo();

		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setOptimizedGenerics(true);
		kryo.setDefaultSerializer(factory);

		kryo.register(ListContainer.class);
		kryo.register(Test.class);
		kryo.register(ArrayList.class);
		kryo.setReferences(false);

		Output output = new Output(1024);
		kryo.writeObject(output, object);
		output.flush();

		Input input = new Input(output.getBuffer(), 0, output.position());
		Object object2 = kryo.readObject(input, object.getClass());

		System.out.println("Size: " + output.position());
		return (T)object2;
	}
}
