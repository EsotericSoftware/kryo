package com.esotericsoftware.kryo.benchmarks.utils;

import com.esotericsoftware.kryo.util.*;

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
public class FastGetIntMapBenchmark {
    
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
        @Param({"500", "1000", "3000", "10000"}) public int numClasses;
        @Param({"2048"}) public int maxCapacity;
        @Param({"intMap", "fastGetIntMap"}) public MapType mapType;
        
        IntObjectMapAdapter<Object> map;
        Object[] classes;
        List<Integer> integers;
    }
    
    @State(Scope.Thread)
    public static class BenchmarkState extends AbstractBenchmarkState {
        
        @Setup(Level.Trial)
        public void setup () {
            map = createMap(mapType, maxCapacity);
            classes = IntStream.rangeClosed(0, numClasses).mapToObj(FastGetIntMapBenchmark::buildClass).toArray();
            integers = IntStream.rangeClosed(0, numClasses).boxed().collect(Collectors.toList());
        }
        
        public void write (Blackhole blackhole) {
            integers.stream()
                    .map(id -> map.put(id, classes[id]))
                    .forEach(blackhole::consume);
        }
        
        public void readWrite (Blackhole blackhole) {
            // read
            integers.stream()
                    .map(id -> map.put(id, classes[id]))
                    .forEach(blackhole::consume);
            
            final Random random = new Random();
            for (int i = 0; i < numClasses; i++) {
                int key = random.nextInt(numClasses - 1);
                blackhole.consume(map.get(key));
            }
            
            map.clear();
        }
    }
    
    @State(Scope.Thread)
    public static class ReadBenchmarkState extends AbstractBenchmarkState {
        
        @Setup(Level.Iteration)
        public void setup () {
            map = createMap(mapType, maxCapacity);
            classes = IntStream.rangeClosed(0, numClasses).mapToObj(FastGetIntMapBenchmark::buildClass).toArray();
            integers = IntStream.rangeClosed(0, numClasses).boxed().collect(Collectors.toList());
        }
        
        @Setup(Level.Invocation)
        public void shuffle(){
            Collections.shuffle(integers);
        }
        
        public void read (Blackhole blackhole) {
            integers.stream()
                    .limit(500)
                    .map(map::get)
                    .forEach(blackhole::consume);
        }
    }
    
    public enum MapType {
        fastGetIntMap, intMap
    }
    
    
    
    interface IntObjectMapAdapter<V> {
        V get (int key);
        
        V put (int key, V value);
        
        void clear();
        
    }
    
    static class IntMapAdapter<V> implements IntObjectMapAdapter<V> {
        private final IntMap<V> delegate;
        private final int maxCapacity;
        
        public IntMapAdapter (IntMap<V> delegate, int maxCapacity) {
            this.delegate = delegate;
            this.maxCapacity = maxCapacity;
        }
        
        @Override
        public V get (int key) {
            return delegate.get(key, null);
        }
        
        @Override
        public V put (int key, V value) {
            delegate.put(key, value);
            return null;
        }
    
        @Override
        public void clear() {
            delegate.clear(maxCapacity);
        }
    }
    
    static class FstGetIntMapAdapter<V> implements IntObjectMapAdapter<V> {
        private final FastGetIntMap<V> delegate;
        private final int maxCapacity;
        
        public FstGetIntMapAdapter (FastGetIntMap<V> delegate, int maxCapacity) {
            this.delegate = delegate;
            this.maxCapacity = maxCapacity;
        }
        
        @Override
        public V get (int key) {
            return delegate.get(key, null);
        }
        
        @Override
        public V put (int key, V value) {
            delegate.put(key, value);
            return null;
        }
        
        @Override
        public void clear() {
            delegate.clear(maxCapacity);
        }
    }
    
    
    private static IntObjectMapAdapter<Object> createMap (MapType mapType, int maxCapacity) {
        switch (mapType) {
            case intMap:
                return new IntMapAdapter<>(new IntMap<>(), maxCapacity);
            case fastGetIntMap:
                return new FstGetIntMapAdapter<>(new FastGetIntMap<>(), maxCapacity);
            default:
                throw new IllegalStateException("Unexpected value: " + mapType);
        }
    }
    
    
    private static Class<?> buildClass (int i) {
        return new ByteBuddy()
                .subclass(Object.class)
                .method(ElementMatchers.named("toString"))
                .intercept(FixedValue.value(String.valueOf(i)))
                .make()
                .load(FastGetIntMapBenchmark.class.getClassLoader())
                .getLoaded();
    }
    
    
    
    
}