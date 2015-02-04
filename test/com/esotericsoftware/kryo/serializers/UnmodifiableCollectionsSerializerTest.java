package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.KryoTestCase;

import java.util.*;

/**
 * @author Sebastian Bathke <sebastian.bathke@gmail.com>
 */
public class UnmodifiableCollectionsSerializerTest extends KryoTestCase {

    /**
     * This assumes UnmodifiableCollectionsSerializer is added as default serializer.
     */
    public void testUnmodifiableCollectionsSerializer () {
        kryo.setRegistrationRequired(false);

        List<?> testList = Collections.unmodifiableList(new ArrayList<>(0));
        roundTrip(75, 78, testList);

        Map<?,?> testMap = Collections.unmodifiableMap(new HashMap<>(0));
        roundTrip(60, 66, testMap);

        Set<?> testSet = Collections.unmodifiableSet(new HashSet<>(0));
        roundTrip(60, 63, testSet);
    }
}
