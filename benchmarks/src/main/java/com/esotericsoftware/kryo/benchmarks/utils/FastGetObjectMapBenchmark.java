package com.esotericsoftware.kryo.benchmarks.utils;

import com.esotericsoftware.kryo.util.CuckooObjectMap;

import com.esotericsoftware.kryo.util.FastGetObjectMap;
import com.esotericsoftware.kryo.util.ObjectMap;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;

//  mvn -f benchmarks/pom.xml compile exec:java -Dexec.args="-f 3 -wi 6 -i 3 -t 2 -w 2s -r 2 FastGetObjectMapBenchmark.read"
public class FastGetObjectMapBenchmark {
    
    @Benchmark
    public void read (ReadBenchmarkState state, Blackhole blackhole) {
        state.read(blackhole);
    }
    
    @Benchmark
    public void write (BenchmarkState state, Blackhole blackhole) {
        state.write(blackhole);
    }
    
    @Benchmark
    public void writeRead (BenchmarkState state, Blackhole blackhole) {
        state.readWrite(blackhole);
    }
    
    @State(Scope.Thread)
    public static class AbstractBenchmarkState {
        @Param({"100"}) public int numClasses;
        @Param({"2048"}) public int maxCapacity;
        @Param({"fastGet", "cuckoo"}) public MapType mapType;
        
        MapAdapter<Object, Integer> map;
        List<? extends Class<?>> classes;
    }
    
    @State(Scope.Thread)
    public static class BenchmarkState extends AbstractBenchmarkState {
        
        @Setup(Level.Trial)
        public void setup () {
            map = createMap(mapType, maxCapacity);
            classes = IntStream.rangeClosed(0, numClasses).mapToObj(FastGetObjectMapBenchmark::buildClass)
                    .collect(Collectors.toList());
        }
        
        public void write (Blackhole blackhole) {
            classes.stream()
                    .map(c -> map.put(c, 1))
                    .forEach(blackhole::consume);
        }
        
        public void readWrite (Blackhole blackhole) {
            classes.forEach(c -> map.put(c, 1));
            Collections.shuffle(classes);
            
            final Random random = new Random();
            for (int i = 0; i < numClasses; i++) {
                final Class<?> key = classes.get(random.nextInt(numClasses - 1));
                blackhole.consume(map.get(key));
            }
            
            map.clear();
        }
    }
    
    @State(Scope.Thread)
    public static class ReadBenchmarkState extends AbstractBenchmarkState {
        
        @Setup(Level.Trial)
        public void setup () {
            map = createMap(mapType, maxCapacity);
            classes = IntStream.rangeClosed(0, numClasses).mapToObj(FastGetObjectMapBenchmark::buildClass)
                    .collect(Collectors.toList());
            classes.forEach(c -> map.put(c, 1));
            Collections.shuffle(classes);
        }
        
        public void read (Blackhole blackhole) {
            classes.stream()
                    .limit(500)
                    .map(map::get)
                    .forEach(blackhole::consume);
        }
    }
    
    public enum MapType {
     cuckoo, fastGet
    }
    
    
    interface MapAdapter<K, V> {
        V get (K key);
        
        V put (K key, V value);
        
        void clear();
    }
    
    private static MapAdapter<Object, Integer> createMap (MapType mapType, int maxCapacity) {
        switch (mapType) {
            case fastGet:
                return new ObjectMapAdapter<>(new FastGetObjectMap<>(), maxCapacity);
            case cuckoo:
                return new CuckooMapAdapter<>(new CuckooObjectMap<>(), maxCapacity);
            default:
                throw new IllegalStateException("Unexpected value: " + mapType);
        }
    }
    
    static class CuckooMapAdapter<K> implements MapAdapter<K, Integer> {
        private final CuckooObjectMap<K, Integer> delegate;
        private final int maxCapacity;
        
        public CuckooMapAdapter (CuckooObjectMap<K, Integer> delegate, int maxCapacity) {
            this.delegate = delegate;
            this.maxCapacity = maxCapacity;
        }
        
        @Override
        public Integer get (K key) {
            return delegate.get(key, -1);
        }
        
        @Override
        public Integer put (K key, Integer value) {
            delegate.put(key, value);
            return null;
        }
        
        @Override
        public void clear() {
            delegate.clear(maxCapacity);
        }
        
    }
    
    static class ObjectMapAdapter<K> implements MapAdapter<K, Integer> {
        private final ObjectMap<K, Integer> delegate;
        private final int maxCapacity;
        
        public ObjectMapAdapter (ObjectMap<K, Integer> delegate, int maxCapacity) {
            this.delegate = delegate;
            this.maxCapacity = maxCapacity;
        }
        
        @Override
        public Integer get (K key) {
            return delegate.get(key, -1);
        }
        
        @Override
        public Integer put (K key, Integer value) {
            delegate.put(key, value);
            return null;
        }
        
        @Override
        public void clear() {
            delegate.clear(maxCapacity);
        }
        
    }
    
    private static class HashMapAdapter<K> implements MapAdapter<K, Integer> {
        private final HashMap<K, Integer> delegate;
        
        public HashMapAdapter (HashMap<K, Integer> delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public Integer get (K key) {
            return delegate.get(key);
        }
        
        @Override
        public Integer put (K key, Integer value) {
            return delegate.put(key, value);
        }
        
        @Override
        public void clear() {
            delegate.clear();
        }
    }
    
    private static Class<?> buildClass (int i) {
        return new ByteBuddy()
                .subclass(Object.class)
                .method(ElementMatchers.named("toString"))
                .intercept(FixedValue.value(String.valueOf(i)))
                .make()
                .load(FastGetObjectMapBenchmark.class.getClassLoader())
                .getLoaded();
    }
    
    
    
    
}