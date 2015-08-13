package com.esotericsoftware.kryo;

import java.util.Arrays;

import org.objenesis.strategy.InstantiatorStrategy;

import com.esotericsoftware.kryo.factories.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.DefaultStreamFactory;
import com.esotericsoftware.kryo.util.MapReferenceResolver;

public final class KryoBuilder implements KryoFactory {

	private static enum Step {
		NEW_INSTANCE,
		SET_DEFAULT_SERIALIZER,
		ADD_DEFAULT_SERIALIZER,
		REGISTER,
		SET_CLASSLOADER,
		SET_REGISTRATION_REQUIRED,
		SET_REFERENCES,
		SET_COPY_REFERENCES,
		SET_REFERENCE_RESOLVER,
		SET_INSTANTIATOR_STRATEGY,
		SET_AUTO_RESET,
		SET_MAX_DEPTH,
		SET_STREAM_FACTORY,
		SET_ASM_ENABLED,
		
		;
		
		private static final Step[] VALUES = values();
	}
	
	public static final class KryoBuilderSerializer extends Serializer<KryoBuilder> {

		@SuppressWarnings("synthetic-access")
		@Override
		public void write (Kryo kryo, Output output, KryoBuilder object) {
			output.writeInt(object.step.ordinal(), true);
			output.writeInt(object.args.length, true);
			for(int i = 0; i < object.args.length; i++)
				kryo.writeClassAndObject(output, object.args[i]);
			kryo.writeObjectOrNull(output, object.tail, KryoBuilder.class);
		}

		@SuppressWarnings("synthetic-access")
		@Override
		public KryoBuilder read (Kryo kryo, Input input, Class<KryoBuilder> type) {
			KryoBuilder object = new KryoBuilder(null, null, null, null);
			kryo.reference(object);
			
			object.step = Step.VALUES[input.readInt(true)];
			object.args = new Object[input.readInt(true)];
			for(int i = 0; i < object.args.length; i++)
				object.args[i] = kryo.readClassAndObject(input);
			object.tail = kryo.readObjectOrNull(input, KryoBuilder.class);

			if(object.step == Step.NEW_INSTANCE) {
				for(KryoBuilder h = object; h != null; h = h.tail)
					h.head = object;
			}
			
			return object;
		}
		
	}
	
	private KryoBuilder head;
	private KryoBuilder tail;
	private Step step;
	private Object[] args;

	public KryoBuilder () {
		this((ClassResolver) null, (ReferenceResolver) null, (StreamFactory) null);
	}

	public KryoBuilder (ReferenceResolver referenceResolver) {
		this((ClassResolver) null, referenceResolver, (StreamFactory) null);
	}

	public KryoBuilder (ClassResolver classResolver, ReferenceResolver referenceResolver) {
		this(classResolver, referenceResolver, (StreamFactory) null);
	}
	
	public KryoBuilder(ClassResolver classResolver, ReferenceResolver referenceResolver, StreamFactory streamFactory) {
		head = this;
		tail = null;
		step = Step.NEW_INSTANCE;
		args = new Object[] {classResolver, referenceResolver, streamFactory};
	}
	
	public KryoBuilder(KryoFactory factory) {
		head = this;
		tail = null;
		step = Step.NEW_INSTANCE;
		args = new Object[] { factory };
	}
	
	private KryoBuilder(KryoBuilder prev, Step step, Object... args) {
		head = prev.head;
		tail = null;
		this.step = step;
		this.args = args;
		while(prev.tail != null)
			prev = prev.tail;
		prev.tail = this;
	}
	
	private KryoBuilder(KryoBuilder head, KryoBuilder tail, Step step, Object[] args) {
		this.head = head;
		this.tail = tail;
		this.step = step;
		this.args = args;
	}
	
	@Override
	public boolean equals (Object obj) {
		if(obj == this)
			return true;
		if(obj instanceof KryoBuilder) {
			KryoBuilder h1 = this.head;
			KryoBuilder h2 = ((KryoBuilder) obj).head;
			while(h1 != null && h2 != null) {
				if(h1.step != h2.step)
					return false;
				if(!Arrays.equals(h1.args, h2.args))
					return false;
				h1 = h1.tail;
				h2 = h2.tail;
			}
			return h1 == null && h2 == null;
		}
		return false;
	}
	
	@Override
	public int hashCode () {
		int hash = 0;
		KryoBuilder h = this.head;
		while(h != null) {
			hash = 31 * hash + h.step.hashCode() + Arrays.hashCode(h.args);
			h = h.tail;
		}
		return hash;
	}
	
	public Kryo create() {
		return head.step(null);
	}
	
	public <T extends Kryo> T configure(T kryo) {
		return (T) head.step(kryo);
	}
	
	private boolean argtypes(Class<?>... types) {
		if(args.length != types.length)
			return false;
		for(int i = 0; i < args.length; i++)
			if(args[i] != null && !types[i].isInstance(args[i]))
				return false;
		return true;
	}
	
	private Kryo step(Kryo kryo) {
		switch(step) {
		case NEW_INSTANCE:
			if(kryo == null) {
				if(argtypes(KryoFactory.class))
					kryo = ((KryoFactory) args[0]).create();
				else if(argtypes(ClassResolver.class, ReferenceResolver.class, StreamFactory.class)) {
					Object[] args = this.args.clone();
					if(args[0] == null)
						args[0] = new DefaultClassResolver();
					if(args[1] == null)
						args[1] = new MapReferenceResolver();
					if(args[2] == null)
						args[2] = new DefaultStreamFactory();
					kryo = new Kryo((ClassResolver) args[0], (ReferenceResolver) args[1], (StreamFactory) args[2]);
				} else
					throw new IllegalStateException();
			}
			break;
		case SET_DEFAULT_SERIALIZER:
			if(argtypes(SerializerFactory.class))
				kryo.setDefaultSerializer((SerializerFactory) args[0]);
			else if(argtypes(Serializer.class))
				kryo.setDefaultSerializer(((Class<?>) args[0]).asSubclass((Serializer.class)));
			else
				throw new IllegalStateException();
			break;
		case ADD_DEFAULT_SERIALIZER:
			if(argtypes(Class.class, Serializer.class))
				kryo.addDefaultSerializer((Class<?>) args[0], (Serializer) args[1]);
			else if(argtypes(Class.class, SerializerFactory.class))
				kryo.addDefaultSerializer((Class<?>) args[0], (SerializerFactory) args[1]);
			else if(argtypes(Class.class, Class.class))
				kryo.addDefaultSerializer((Class<?>) args[0], ((Class<?>) args[1]).asSubclass(Serializer.class));
			else
				throw new IllegalStateException();
			break;
		case REGISTER:
			if(argtypes(Class.class))
				kryo.register((Class<?>) args[0]);
			else if(argtypes(Class.class, Integer.class))
				kryo.register((Class<?>) args[0], (Integer) args[1]);
			else if(argtypes(Class.class, Serializer.class))
				kryo.register((Class<?>) args[0], (Serializer) args[1]);
			else if(argtypes(Class.class, Serializer.class, Integer.class))
				kryo.register((Class<?>) args[0], (Serializer) args[1], (Integer) args[2]);
			else if(argtypes(Registration.class))
				kryo.register((Registration) args[0]);
			else
				throw new IllegalStateException();
			break;
		case SET_CLASSLOADER:
			if(argtypes(ClassLoader.class))
				kryo.setClassLoader((ClassLoader) args[0]);
			else
				throw new IllegalStateException();
			break;
		case SET_REGISTRATION_REQUIRED:
			if(argtypes(Boolean.class))
				kryo.setRegistrationRequired((Boolean) args[0]);
			else
				throw new IllegalStateException();
			break;
		case SET_REFERENCES:
			if(argtypes(Boolean.class))
				kryo.setReferences((Boolean) args[0]);
			else
				throw new IllegalStateException();
			break;
		case SET_COPY_REFERENCES:
			if(argtypes(Boolean.class))
				kryo.setCopyReferences((Boolean) args[0]);
			else
				throw new IllegalStateException();
			break;
		case SET_REFERENCE_RESOLVER:
			if(argtypes(ReferenceResolver.class))
				kryo.setReferenceResolver((ReferenceResolver) args[0]);
			else
				throw new IllegalStateException();
			break;
		case SET_INSTANTIATOR_STRATEGY:
			if(argtypes(InstantiatorStrategy.class))
				kryo.setInstantiatorStrategy((InstantiatorStrategy) args[0]);
			else
				throw new IllegalStateException();
			break;
		case SET_AUTO_RESET:
			if(argtypes(Boolean.class))
				kryo.setAutoReset((Boolean) args[0]);
			else
				throw new IllegalStateException();
			break;
		case SET_MAX_DEPTH:
			if(argtypes(Integer.class))
				kryo.setMaxDepth((Integer) args[0]);
			else
				throw new IllegalStateException();
			break;
		case SET_STREAM_FACTORY:
			if(argtypes(StreamFactory.class))
				kryo.setStreamFactory((StreamFactory) args[0]);
			else
				throw new IllegalStateException();
			break;
		case SET_ASM_ENABLED:
			if(argtypes(Boolean.class))
				kryo.setAsmEnabled((Boolean) args[0]);
			else
				throw new IllegalStateException();
			break;
		}
		return tail != null ? tail.step(kryo) : kryo;
	}

	public KryoBuilder setDefaultSerializer (SerializerFactory serializer) {
		return new KryoBuilder(this, Step.SET_DEFAULT_SERIALIZER, serializer);
	}

	public KryoBuilder setDefaultSerializer (Class<? extends Serializer> serializer) {
		return new KryoBuilder(this, Step.SET_DEFAULT_SERIALIZER, serializer);
	}

	public KryoBuilder addDefaultSerializer (Class type, Serializer serializer) {
		return new KryoBuilder(this, Step.ADD_DEFAULT_SERIALIZER, type, serializer);
	}

	public KryoBuilder addDefaultSerializer (Class type, SerializerFactory serializerFactory) {
		return new KryoBuilder(this, Step.ADD_DEFAULT_SERIALIZER, type, serializerFactory);
	}

	public KryoBuilder addDefaultSerializer (Class type, Class<? extends Serializer> serializerClass) {
		return new KryoBuilder(this, Step.ADD_DEFAULT_SERIALIZER, type, serializerClass);
	}

	public KryoBuilder register (Class type) {
		return new KryoBuilder(this, Step.REGISTER, type);
	}

	public KryoBuilder register (Class type, int id) {
		return new KryoBuilder(this, Step.REGISTER, type, id);
	}

	public KryoBuilder register (Class type, Serializer serializer) {
		return new KryoBuilder(this, Step.REGISTER, type, serializer);
	}

	public KryoBuilder register (Class type, Serializer serializer, int id) {
		return new KryoBuilder(this, Step.REGISTER, type, serializer, id);
	}

	public KryoBuilder register (Registration registration) {
		return new KryoBuilder(this, Step.REGISTER, registration);
	}

	public KryoBuilder setClassLoader (ClassLoader classLoader) {
		return new KryoBuilder(this, Step.SET_CLASSLOADER, classLoader);
	}

	public KryoBuilder setRegistrationRequired (boolean registrationRequired) {
		return new KryoBuilder(this, Step.SET_REGISTRATION_REQUIRED, registrationRequired);
	}

	public KryoBuilder setReferences (boolean references) {
		return new KryoBuilder(this, Step.SET_REFERENCES, references);
	}

	public KryoBuilder setCopyReferences (boolean copyReferences) {
		return new KryoBuilder(this, Step.SET_COPY_REFERENCES, copyReferences);
	}

	public KryoBuilder setReferenceResolver (ReferenceResolver referenceResolver) {
		return new KryoBuilder(this, Step.SET_REFERENCE_RESOLVER, referenceResolver);
	}

	public KryoBuilder setInstantiatorStrategy (InstantiatorStrategy strategy) {
		return new KryoBuilder(this, Step.SET_INSTANTIATOR_STRATEGY, strategy);
	}

	public KryoBuilder setAutoReset (boolean autoReset) {
		return new KryoBuilder(this, Step.SET_AUTO_RESET, autoReset);
	}

	public KryoBuilder setMaxDepth (int maxDepth) {
		return new KryoBuilder(this, Step.SET_MAX_DEPTH, maxDepth);
	}

	public KryoBuilder setStreamFactory (StreamFactory streamFactory) {
		return new KryoBuilder(this, Step.SET_STREAM_FACTORY, streamFactory);
	}

	public KryoBuilder setAsmEnabled (boolean flag) {
		return new KryoBuilder(this, Step.SET_ASM_ENABLED, flag);
	}
}
