package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * You need run encode() first. And then run decode() follow instruction.
 *
 * Create by maxisvest
 * 2019/7/25 14:20
 */
public class ClassStructureTest extends KryoTestCase {

    public static final Kryo kryo = new Kryo();

    @Before
    public void setUp () throws Exception {
        super.setUp();
        SerializerFactory.CompatibleFieldSerializerFactory compatibleFieldSerializerFactory = new SerializerFactory.CompatibleFieldSerializerFactory();
        CompatibleFieldSerializer.CompatibleFieldSerializerConfig config = compatibleFieldSerializerFactory.getConfig();
        kryo.setDefaultSerializer(compatibleFieldSerializerFactory);
        kryo.setRegistrationRequired(false);

        //this case use for enable references and chunked encoding
        kryo.setReferences(true);
        config.setChunkedEncoding(true);
    }

    /**
     * Run this method first, to write a full version instance of class DataBean.
     *
     * DataBean include SubData and SubData include the String message.
     * If enable kryo.references function, String message in SubData will write first and this String in
     * DataBean will write as a reference.
     *
     * Imagine this procedure is the server want to transfer data to client.
     * @throws IOException
     */
    @Test
    public void encode() throws IOException {
        String message = "Hello Kryo!";

        SubData subData = new SubData(); //delete on decode test
        subData.message = message; //delete on decode test

        DataBean dataBean = new DataBean();
        dataBean.aSubData = subData; //delete on decode test
        dataBean.message = message;

        Output output = new Output(new FileOutputStream("file_y.bin"));
        kryo.writeObject(output, dataBean);
        output.close();
    }

    /**
     * Before run this method, you need delete the line and Class SubData marked as "delete on decode test". The Class DataBean only
     * have the field String message.
     *
     * Imagine this is the client want to deserialize the data come from server. But client use the old version of Class DataBean.
     *
     * Normally, decode() will throw an Exception because it can not find the referenced value of String message. The String message
     * referenced value is in the chunked input but it has been skipped.
     * Use PR #686 will solve this problem and recover the String message in DataBean. Therefore run decode() will pass now.
     *
     * @throws IOException
     */
    @Test
    public void decode() throws IOException {
        File file = new File("file_y.bin");
        if (file.exists()) {
            Input input = new Input(new FileInputStream(file));
            DataBean dataBean = kryo.readObject(input, DataBean.class);
            file.delete();
            input.close();
        }
    }



    static public class DataBean {
       SubData aSubData; //delete on decode test
       String message;
    }

    //delete on decode test
    static public class SubData {
        String message;
    }




}
