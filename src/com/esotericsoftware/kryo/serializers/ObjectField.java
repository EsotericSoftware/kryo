package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.reflectasm.FieldAccess;

/*** Defer generation of serializers until it is really required at run-time. By default, use reflection-based approach. 
 * @author Nathan Sweet <misc@n4te.com>
 * @author Roman Levenstein <romixlev@gmail.com> */
class ObjectField extends CachedField {
	public Class[] generics;
	final FieldSerializer fieldSerializer;
	final Class type;
	final Kryo kryo;

	ObjectField (FieldSerializer fieldSerializer) {
		this.fieldSerializer = fieldSerializer;
		this.kryo = fieldSerializer.kryo;
		this.type = fieldSerializer.type;
	}

	public Object getField (Object object) throws IllegalArgumentException, IllegalAccessException {
		return field.get(object);
	}

	public void setField (Object object, Object value) throws IllegalArgumentException, IllegalAccessException {
		field.set(object, value);
	}

	final public void write (Output output, Object object) {
		try {
			// if(typeVar2concreteClass != null) {
			// // Push a new scope for generics
			// kryo.pushGenericsScope(type, new Generics(typeVar2concreteClass));
			// }

			if (TRACE)
				trace("kryo", "Write field: " + this + " (" + object.getClass().getName() + ")" + " pos=" + output.position());

			Object value = getField(object);

			Serializer serializer = this.serializer;
			if (valueClass == null) {
				// The concrete type of the field is unknown, write the class first.
				if (value == null) {
					kryo.writeClass(output, null);
					return;
				}
				Registration registration = kryo.writeClass(output, value.getClass());
				if (serializer == null) serializer = registration.getSerializer();
				// if (generics != null)
				serializer.setGenerics(kryo, generics);
				kryo.writeObject(output, value, serializer);
			} else {
				// The concrete type of the field is known, always use the same serializer.
				if (serializer == null) this.serializer = serializer = kryo.getSerializer(valueClass);
				// if (generics != null)
				serializer.setGenerics(kryo, generics);
				if (canBeNull) {
					kryo.writeObjectOrNull(output, value, serializer);
				} else {
					if (value == null) {
						throw new KryoException("Field value is null but canBeNull is false: " + this + " (" + object.getClass().getName() + ")");
					}
					kryo.writeObject(output, value, serializer);
				}
			}
		} catch (IllegalAccessException ex) {
			throw new KryoException("Error accessing field: " + this + " (" + object.getClass().getName() + ")", ex);
		} catch (KryoException ex) {
			ex.addTrace(this + " (" + object.getClass().getName() + ")");
			throw ex;
		} catch (RuntimeException runtimeEx) {
			KryoException ex = new KryoException(runtimeEx);
			ex.addTrace(this + " (" + object.getClass().getName() + ")");
			throw ex;
		} finally {
			// if(typeVar2concreteClass != null)
			// kryo.popGenericsScope();
		}
	}

	final public void read (Input input, Object object) {
		try {
			if (TRACE) trace("kryo", "Read field: " + this + " (" + type.getName() + ")" + " pos=" + input.position());
			Object value;

			Class concreteType = valueClass;
			Serializer serializer = this.serializer;
			if (concreteType == null) {
				Registration registration = kryo.readClass(input);
				if (registration == null)
					value = null;
				else {
					if (serializer == null) serializer = registration.getSerializer();
					// if (generics != null)
					serializer.setGenerics(kryo, generics);
					value = kryo.readObject(input, registration.getType(), serializer);
				}
			} else {
				if (serializer == null) this.serializer = serializer = kryo.getSerializer(valueClass);
				// if (generics != null)
				serializer.setGenerics(kryo, generics);
				if (canBeNull)
					value = kryo.readObjectOrNull(input, concreteType, serializer);
				else
					value = kryo.readObject(input, concreteType, serializer);
			}

			setField(object, value);
		} catch (IllegalAccessException ex) {
			throw new KryoException("Error accessing field: " + this + " (" + type.getName() + ")", ex);
		} catch (KryoException ex) {
			ex.addTrace(this + " (" + type.getName() + ")");
			throw ex;
		} catch (RuntimeException runtimeEx) {
			KryoException ex = new KryoException(runtimeEx);
			ex.addTrace(this + " (" + type.getName() + ")");
			throw ex;
		} finally {
			// if(typeVar2concreteClass != null)
			// kryo.popGenericsScope();
		}
	}

	public void copy (Object original, Object copy) {
		try {
			if (accessIndex != -1) {
				FieldAccess access = (FieldAccess)fieldSerializer.access;
				access.set(copy, accessIndex, kryo.copy(access.get(original, accessIndex)));
			} else
				setField(copy, kryo.copy(getField(original)));
		} catch (IllegalAccessException ex) {
			throw new KryoException("Error accessing field: " + this + " (" + type.getName() + ")", ex);
		} catch (KryoException ex) {
			ex.addTrace(this + " (" + type.getName() + ")");
			throw ex;
		} catch (RuntimeException runtimeEx) {
			KryoException ex = new KryoException(runtimeEx);
			ex.addTrace(this + " (" + type.getName() + ")");
			throw ex;
		}
	}

	final static class ObjectIntField extends ObjectField {
		public ObjectIntField (FieldSerializer fieldSerializer) {
			super(fieldSerializer);
		}
		public Object getField (Object object) throws IllegalArgumentException, IllegalAccessException {
			return field.getInt(object);
		}
	}

	final static class ObjectFloatField extends ObjectField {
		public ObjectFloatField (FieldSerializer fieldSerializer) {
			super(fieldSerializer);
		}
		public Object getField (Object object) throws IllegalArgumentException, IllegalAccessException {
			return field.getFloat(object);
		}
	}

	final static class ObjectShortField extends ObjectField {
		public ObjectShortField (FieldSerializer fieldSerializer) {
			super(fieldSerializer);
		}
		public Object getField (Object object) throws IllegalArgumentException, IllegalAccessException {
			return field.getShort(object);
		}
	}
	
	final static class ObjectByteField extends ObjectField {
		public ObjectByteField (FieldSerializer fieldSerializer) {
			super(fieldSerializer);
		}
		public Object getField (Object object) throws IllegalArgumentException, IllegalAccessException {
			return field.getByte(object);
		}
	}

	final static class ObjectBooleanField extends ObjectField {
		public ObjectBooleanField (FieldSerializer fieldSerializer) {
			super(fieldSerializer);
		}
		public Object getField (Object object) throws IllegalArgumentException, IllegalAccessException {
			return field.getBoolean(object);
		}
	}

	final static class ObjectCharField extends ObjectField {
		public ObjectCharField (FieldSerializer fieldSerializer) {
			super(fieldSerializer);
		}
		public Object getField (Object object) throws IllegalArgumentException, IllegalAccessException {
			return field.getChar(object);
		}
	}

	final static class ObjectLongField extends ObjectField {
		public ObjectLongField (FieldSerializer fieldSerializer) {
			super(fieldSerializer);
		}
		public Object getField (Object object) throws IllegalArgumentException, IllegalAccessException {
			return field.getLong(object);
		}
	}

	final static class ObjectDoubleField extends ObjectField {
		public ObjectDoubleField (FieldSerializer fieldSerializer) {
			super(fieldSerializer);
		}
		public Object getField (Object object) throws IllegalArgumentException, IllegalAccessException {
			return field.getDouble(object);
		}
	}
}
