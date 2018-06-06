/* Copyright (c) 2008-2018, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class KryoBenchmarks {
	/** To run, in command-line: $ mvn clean install exec:java -Dexec.args="-wi 10 -i 10" */
	public static void main (String[] args) throws Exception {

		org.apache.commons.cli.Options cliOptions = new org.apache.commons.cli.Options();

		Option includeOption = Option.builder("incl").argName("include").hasArg().build();
		Option forksOption = Option.builder("f").argName("forks").hasArg().build();
		Option warmupIterationsOption = Option.builder("wi").argName("warmupIterations").hasArg().build();
		Option iterationsOption = Option.builder("i").argName("iterations").hasArg().build();
		Option threadsOption = Option.builder("t").argName("threads").hasArg().build();

		cliOptions.addOption(includeOption);
		cliOptions.addOption(forksOption);
		cliOptions.addOption(warmupIterationsOption);
		cliOptions.addOption(iterationsOption);
		cliOptions.addOption(threadsOption);

		CommandLineParser parser = new DefaultParser();
		CommandLine cl = parser.parse(cliOptions, args);

		String include = cl.getOptionValue(includeOption.getOpt(), "Benchmark");
		int forks = intValue(cl, forksOption, "1");
		int warmupIterations = intValue(cl, warmupIterationsOption, "10");
		int iterations = intValue(cl, iterationsOption, "5");
		int threads = intValue(cl, threadsOption, "1");

		Options options = new OptionsBuilder().include(include).forks(forks)
			// .mode(Mode.SampleTime)
			// .timeUnit(TimeUnit.NANOSECONDS)
			.warmupIterations(warmupIterations).measurementIterations(iterations).threads(threads).build();

		//new Runner(options).run();
		Main.main(args);
	}

	private static int intValue (CommandLine cl, Option o, String defaultValue) {
		return Integer.parseInt(cl.getOptionValue(o.getOpt(), defaultValue));
	}
}
