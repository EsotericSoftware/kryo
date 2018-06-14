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

/**
 * Configurable {@link Kryo} builder (implementing {@link KryoFactory}) with a provided {@link Serializer}
 * ({@link KryoBuilderSerializer}) to make the builder itself able to be serialized.<p>
 * 
 * Typical use case is for "self-descriptive" Kryo data.  One would create a {@link Kryo}
 * instance using a {@link KryoBuilder}, then as the first object written, serialize the builder
 * itself.  When deserializing, first deserialize the {@link KryoBuilder}, then from
 * it build a {@link Kryo} to deserialize the remainder of the input.<p>
 * 
 * More advanced uses of {@link Kryo} may require serializing individual {@link Serializer} instances,
 * such as if {@link #register(Class, Serializer)} or {@link #register(Class, Serializer, int)} is
 * called on a {@link KryoBuilder} which must be serialized.  An alternative to serializing
 * {@link Serializer}s is to subclass the {@link Serializer}, performing customizations in the
 * constructor, and instead call {@link #addDefaultSerializer(Class, Class)}.<p>
 * 
 * {@link KryoBuilderSerializer} can serialize a {@link KryoBuilder}, but does not provide
 * explicit support for potentially required objects, such as {@link Serializer} instances
 * that must be registered.<p>
 * 
 * {@link KryoBuilder} instances represent a single step in the customization of a {@link Kryo}.
 * Instances are joined in a linked-list fashion, with a reference to the head of the list,
 * for easy chaining.
 * 
 * @author Robin Kirkman
 *
 */
public final class KryoBuilder implements KryoFactory {

	/**
	 * Steps types taken by a {@link KryoBuilder} upon a {@link Kryo} instance to customize it.
	 * @author robin
	 *
	 */
	private static enum Step {
		/**
		 * Create a new {@link Kryo}
		 */
		NEW_INSTANCE,
		/**
		 * Set the default serializer
		 * @see Kryo#setDefaultSerializer(Class)
		 * @see Kryo#setDefaultSerializer(SerializerFactory)
		 */
		SET_DEFAULT_SERIALIZER,
		/**
		 * Add a per-type default serializer
		 * @see Kryo#addDefaultSerializer(Class, Class)
		 * @see Kryo#addDefaultSerializer(Class, Serializer)
		 * @see Kryo#addDefaultSerializer(Class, SerializerFactory)
		 */
		ADD_DEFAULT_SERIALIZER,
		/**
		 * Register a serializer
		 * @see Kryo#register(Class)
		 * @see Kryo#register(Registration)
		 * @see Kryo#register(Class, int)
		 * @see Kryo#register(Class, Serializer)
		 * @see Kryo#register(Class, Serializer, int)
		 */
		REGISTER,
		/**
		 * Set the classloader
		 * @see Kryo#setClassLoader(ClassLoader)
		 */
		SET_CLASSLOADER,
		/**
		 * Set whether registration is required
		 * @see Kryo#setRegistrationRequired(boolean)
		 */
		SET_REGISTRATION_REQUIRED,
		/**
		 * Sets whether references are used
		 * @see Kryo#setReferences(boolean)
		 */
		SET_REFERENCES,
		/**
		 * Sets whether copy-references are used
		 * @see Kryo#setCopyReferences(boolean)
		 */
		SET_COPY_REFERENCES,
		/**
		 * Sets the reference resolver
		 * @see Kryo#setReferenceResolver(ReferenceResolver)
		 */
		SET_REFERENCE_RESOLVER,
		/**
		 * Sets the instantiator strategy
		 * @see Kryo#setInstantiatorStrategy(InstantiatorStrategy)
		 */
		SET_INSTANTIATOR_STRATEGY,
		/**
		 * Sets auto-reset
		 * @see Kryo#setAutoReset(boolean)
		 */
		SET_AUTO_RESET,
		/**
		 * Sets the max depth
		 * @see Kryo#setMaxDepth(int)
		 */
		SET_MAX_DEPTH,
		/**
		 * Sets the stream factory
		 * @see Kryo#setStreamFactory(StreamFactory)
		 */
		SET_STREAM_FACTORY,
		/**
		 * Sets whether asm is enabled
		 * @see Kryo#setAsmEnabled(boolean)
		 */
		SET_ASM_ENABLED,
		
		;
		
		/**
		 * Cached array of values as returned by {@link #values()}
		 */
		private static final Step[] VALUES = values();
	}
	
	/**
	 * {@link Serializer} for {@link KryoBuilder}.<p>
	 * 
	 * Does not require references.
	 * @author Robin Kirkman
	 *
	 */
	/*
	 * Format is:
	 * 1) step enum ordinal
	 * 2) length of args array
	 * 3) individual argument objects
	 * 4) the next step to be applied (tail)
	 */
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
	
	/*
	 * TODO: head, step, and args could be final if KryoBuilderSerializer can
	 * be assured to never need to use backreferences 
	 */
	
	/**
	 * Reference to the first action to be taken, which should have {@link #step} = {@link Step#NEW_INSTANCE}.
	 * Should never be null except (temporarily) during deserialization.
	 */
	private KryoBuilder head;
	/**
	 * The next step to be taken.  Can be {@code null}
	 */
	private KryoBuilder tail;
	/**
	 * The {@link Step} to apply.
	 * Should never be null except (temporarily) during deserialization
	 */
	private Step step;
	/**
	 * Arguments to the customization step
	 */
	private Object[] args;

	/**
	 * Create a new {@link KryoBuilder} that constructs a {@link Kryo} by
	 * calling {@link Kryo#Kryo()}
	 * @see Kryo#Kryo()
	 */
	public KryoBuilder () {
		this((ClassResolver) null, (ReferenceResolver) null, (StreamFactory) null);
	}

	/**
	 * Create a new {@link KryoBuilder} that constructs a {@link Kryo} by
	 * calling {@link Kryo#Kryo(ReferenceResolver)}
	 * @param referenceResolver The reference resolver
	 * @see Kryo#Kryo(ReferenceResolver)
	 */
	public KryoBuilder (ReferenceResolver referenceResolver) {
		this((ClassResolver) null, referenceResolver, (StreamFactory) null);
	}

	/**
	 * Create a new {@link KryoBuilder} that constructs a {@link Kryo} by
	 * calling {@link Kryo#Kryo(ClassResolver, ReferenceResolver)}
	 * @param classResolver The class resolver
	 * @param referenceResolver The reference resolver
	 * @see Kryo#Kryo(ClassResolver, ReferenceResolver)
	 */
	public KryoBuilder (ClassResolver classResolver, ReferenceResolver referenceResolver) {
		this(classResolver, referenceResolver, (StreamFactory) null);
	}
	
	/**
	 * Create a new {@link KryoBuilder} that constructs a {@link Kryo} by
	 * calling {@link Kryo#Kryo(ClassResolver, ReferenceResolver, StreamFactory)}
	 * @param classResolver The class resolver
	 * @param referenceResolver The reference resolver
	 * @param streamFactory The stream factory
	 * @see Kryo#Kryo(ClassResolver, ReferenceResolver, StreamFactory)
	 */
	public KryoBuilder(ClassResolver classResolver, ReferenceResolver referenceResolver, StreamFactory streamFactory) {
		head = this;
		tail = null;
		step = Step.NEW_INSTANCE;
		args = new Object[] {classResolver, referenceResolver, streamFactory};
	}
	
	/**
	 * Create a new {@link KryoBuilder} that constructs a {@link Kryo}
	 * by calling {@link KryoFactory#create()} on the argument
	 * @param factory The factory for new {@link Kryo}s
	 * @see KryoFactory#create()
	 */
	public KryoBuilder(KryoFactory factory) {
		head = this;
		tail = null;
		step = Step.NEW_INSTANCE;
		args = new Object[] { factory };
	}
	
	/**
	 * Constructor used when chaining a new step
	 * @param prev The previous step
	 * @param step The {@link Step} to chain
	 * @param args Arguments to the {@link Step}
	 */
	private KryoBuilder(KryoBuilder prev, Step step, Object... args) {
		head = prev.head;
		tail = null;
		this.step = step;
		this.args = args;
		while(prev.tail != null)
			prev = prev.tail;
		prev.tail = this;
	}
	
	/**
	 * Constructor used during deserialization by {@link KryoBuilderSerializer}
	 * @param head should be {@code null}
	 * @param tail should be {@code null}
	 * @param step should be {@code null}
	 * @param args should be {@code null}
	 */
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

	/**
	 * Create a new {@link Kryo}, applying the steps from this builder
	 * @return A new {@link Kryo}
	 */
	@Override
	public Kryo create() {
		return head.step(null);
	}
	
	/**
	 * Configure an existing {@link Kryo}, applying the steps from this builder
	 * @param kryo
	 * @return
	 */
	public <T extends Kryo> T configure(T kryo) {
		return (T) head.step(kryo);
	}
	
	/**
	 * Returns whether the objects in {@link #args} are all either instances of
	 * the argument types (in order) or {@code null}
	 * @param types The argument types to test for
	 * @return {@code true} if each value in {@link #args} could be cast to the corresponding argument type
	 */
	private boolean argtypes(Class<?>... types) {
		if(args.length != types.length)
			return false;
		for(int i = 0; i < args.length; i++)
			if(args[i] != null && !types[i].isInstance(args[i]))
				return false;
		return true;
	}
	
	/**
	 * Throws an {@link IllegalStateException} if a step cannot be applied
	 * @return never returns
	 */
	private IllegalStateException illegalStep() throws IllegalStateException {
		throw new IllegalStateException("Cannot apply " + step + " with arguments " + Arrays.toString(args));
	}
	
	/**
	 * Apply the configuration step for this {@link KryoBuilder} to the argument
	 * @param kryo The {@link Kryo} to configure.  May be {@code null} for {@link Step#NEW_INSTANCE}
	 * @return The configured {@link Kryo}, possibly created if {@link Step#NEW_INSTANCE}
	 */
	private Kryo step(Kryo kryo) {
		switch(step) {
		case NEW_INSTANCE:
			if(kryo == null) { 
				// create a new Kryo
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
					throw illegalStep();
			} else 
				; // customize the provided argument Kryo
			break;
		case SET_DEFAULT_SERIALIZER:
			if(argtypes(SerializerFactory.class))
				kryo.setDefaultSerializer((SerializerFactory) args[0]);
			else if(argtypes(Serializer.class))
				kryo.setDefaultSerializer(((Class<?>) args[0]).asSubclass((Serializer.class)));
			else
				throw illegalStep();
			break;
		case ADD_DEFAULT_SERIALIZER:
			if(argtypes(Class.class, Serializer.class))
				kryo.addDefaultSerializer((Class<?>) args[0], (Serializer) args[1]);
			else if(argtypes(Class.class, SerializerFactory.class))
				kryo.addDefaultSerializer((Class<?>) args[0], (SerializerFactory) args[1]);
			else if(argtypes(Class.class, Class.class))
				kryo.addDefaultSerializer((Class<?>) args[0], ((Class<?>) args[1]).asSubclass(Serializer.class));
			else
				throw illegalStep();
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
				throw illegalStep();
			break;
		case SET_CLASSLOADER:
			if(argtypes(ClassLoader.class))
				kryo.setClassLoader((ClassLoader) args[0]);
			else
				throw illegalStep();
			break;
		case SET_REGISTRATION_REQUIRED:
			if(argtypes(Boolean.class))
				kryo.setRegistrationRequired((Boolean) args[0]);
			else
				throw illegalStep();
			break;
		case SET_REFERENCES:
			if(argtypes(Boolean.class))
				kryo.setReferences((Boolean) args[0]);
			else
				throw illegalStep();
			break;
		case SET_COPY_REFERENCES:
			if(argtypes(Boolean.class))
				kryo.setCopyReferences((Boolean) args[0]);
			else
				throw illegalStep();
			break;
		case SET_REFERENCE_RESOLVER:
			if(argtypes(ReferenceResolver.class))
				kryo.setReferenceResolver((ReferenceResolver) args[0]);
			else
				throw illegalStep();
			break;
		case SET_INSTANTIATOR_STRATEGY:
			if(argtypes(InstantiatorStrategy.class))
				kryo.setInstantiatorStrategy((InstantiatorStrategy) args[0]);
			else
				throw illegalStep();
			break;
		case SET_AUTO_RESET:
			if(argtypes(Boolean.class))
				kryo.setAutoReset((Boolean) args[0]);
			else
				throw illegalStep();
			break;
		case SET_MAX_DEPTH:
			if(argtypes(Integer.class))
				kryo.setMaxDepth((Integer) args[0]);
			else
				throw illegalStep();
			break;
		case SET_STREAM_FACTORY:
			if(argtypes(StreamFactory.class))
				kryo.setStreamFactory((StreamFactory) args[0]);
			else
				throw illegalStep();
			break;
		case SET_ASM_ENABLED:
			if(argtypes(Boolean.class))
				kryo.setAsmEnabled((Boolean) args[0]);
			else
				throw illegalStep();
			break;
		}
		return tail != null ? tail.step(kryo) : kryo;
	}

	/**
	 * Step to call {@link Kryo#setDefaultSerializer(SerializerFactory)}
	 * @param serializer
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder setDefaultSerializer (SerializerFactory serializer) {
		return new KryoBuilder(this, Step.SET_DEFAULT_SERIALIZER, serializer);
	}

	/**
	 * Step to call {@link Kryo#setDefaultSerializer(Class)}
	 * @param serializer
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder setDefaultSerializer (Class<? extends Serializer> serializer) {
		return new KryoBuilder(this, Step.SET_DEFAULT_SERIALIZER, serializer);
	}

	/**
	 * Step to call {@link Kryo#addDefaultSerializer(Class, Serializer)}
	 * @param type
	 * @param serializer
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder addDefaultSerializer (Class type, Serializer serializer) {
		return new KryoBuilder(this, Step.ADD_DEFAULT_SERIALIZER, type, serializer);
	}

	/**
	 * Step to call {@link Kryo#addDefaultSerializer(Class, SerializerFactory)}
	 * @param type
	 * @param serializerFactory
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder addDefaultSerializer (Class type, SerializerFactory serializerFactory) {
		return new KryoBuilder(this, Step.ADD_DEFAULT_SERIALIZER, type, serializerFactory);
	}

	/**
	 * Step to call {@link Kryo#addDefaultSerializer(Class, Class)}
	 * @param type
	 * @param serializerClass
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder addDefaultSerializer (Class type, Class<? extends Serializer> serializerClass) {
		return new KryoBuilder(this, Step.ADD_DEFAULT_SERIALIZER, type, serializerClass);
	}

	/**
	 * Step to call {@link Kryo#register(Class)}
	 * @param type
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder register (Class type) {
		return new KryoBuilder(this, Step.REGISTER, type);
	}

	/**
	 * Step to call {@link Kryo#register(Class, int)}
	 * @param type
	 * @param id
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder register (Class type, int id) {
		return new KryoBuilder(this, Step.REGISTER, type, id);
	}

	/**
	 * Step to call {@link Kryo#register(Class, Serializer)}
	 * @param type
	 * @param serializer
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder register (Class type, Serializer serializer) {
		return new KryoBuilder(this, Step.REGISTER, type, serializer);
	}

	/**
	 * Step to call {@link Kryo#register(Class, Serializer, int)}
	 * @param type
	 * @param serializer
	 * @param id
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder register (Class type, Serializer serializer, int id) {
		return new KryoBuilder(this, Step.REGISTER, type, serializer, id);
	}

	/**
	 * Step to call {@link Kryo#register(Registration)}
	 * @param registration
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder register (Registration registration) {
		return new KryoBuilder(this, Step.REGISTER, registration);
	}

	/**
	 * Step to call {@link Kryo#setClassLoader(ClassLoader)}
	 * @param classLoader
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder setClassLoader (ClassLoader classLoader) {
		return new KryoBuilder(this, Step.SET_CLASSLOADER, classLoader);
	}

	/**
	 * Step to call {@link Kryo#setRegistrationRequired(boolean)}
	 * @param registrationRequired
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder setRegistrationRequired (boolean registrationRequired) {
		return new KryoBuilder(this, Step.SET_REGISTRATION_REQUIRED, registrationRequired);
	}

	/**
	 * Step to call {@link Kryo#setReferences(boolean)}
	 * @param references
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder setReferences (boolean references) {
		return new KryoBuilder(this, Step.SET_REFERENCES, references);
	}

	/**
	 * Step to call {@link Kryo#setCopyReferences(boolean)}
	 * @param copyReferences
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder setCopyReferences (boolean copyReferences) {
		return new KryoBuilder(this, Step.SET_COPY_REFERENCES, copyReferences);
	}

	/**
	 * Step to call {@link Kryo#setReferenceResolver(ReferenceResolver)}
	 * @param referenceResolver
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder setReferenceResolver (ReferenceResolver referenceResolver) {
		return new KryoBuilder(this, Step.SET_REFERENCE_RESOLVER, referenceResolver);
	}

	/**
	 * Step to call {@link Kryo#setInstantiatorStrategy(InstantiatorStrategy)}
	 * @param strategy
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder setInstantiatorStrategy (InstantiatorStrategy strategy) {
		return new KryoBuilder(this, Step.SET_INSTANTIATOR_STRATEGY, strategy);
	}

	/**
	 * Step to call {@link Kryo#setAutoReset(boolean)}
	 * @param autoReset
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder setAutoReset (boolean autoReset) {
		return new KryoBuilder(this, Step.SET_AUTO_RESET, autoReset);
	}

	/**
	 * Step to call {@link Kryo#setMaxDepth(int)}
	 * @param maxDepth
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder setMaxDepth (int maxDepth) {
		return new KryoBuilder(this, Step.SET_MAX_DEPTH, maxDepth);
	}

	/**
	 * Step to call {@link Kryo#setStreamFactory(StreamFactory)}
	 * @param streamFactory
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder setStreamFactory (StreamFactory streamFactory) {
		return new KryoBuilder(this, Step.SET_STREAM_FACTORY, streamFactory);
	}

	/**
	 * Step to call {@link Kryo#setAsmEnabled(boolean)}
	 * @param flag
	 * @return A chained {@link KryoBuilder} instance
	 */
	public KryoBuilder setAsmEnabled (boolean flag) {
		return new KryoBuilder(this, Step.SET_ASM_ENABLED, flag);
	}
}
