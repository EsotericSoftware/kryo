"""Usage: state-generator.py <source dir> [class count] [--no-strings]

Generates a synthetic "complex application state" object model for benchmarking Kryo.

Deterministic (seeded). Classes are arranged in tiers: tier 0 has only scalar fields, higher tiers reference lower tiers
directly, through List/Map fields and through abstract base types (polymorphic fields). No cycles, so references=false works.
"""
import os, random, sys

NO_STRINGS = "--no-strings" in sys.argv
sys.argv = [a for a in sys.argv if a != "--no-strings"]
OUT = sys.argv[1]
PKG = "com.esotericsoftware.kryo.benchmarks.state"
rnd = random.Random(42)

# Total class count (default 78), split into tiers 0..3 with proportions 30:25:15:8. One class hierarchy (abstract base +
# 2-4 subclasses) per ~20 classes, in tiers 0 and 1.
TOTAL = int(sys.argv[2]) if len(sys.argv) > 2 else 78
TIERS = [max(2, round(TOTAL * w / 78)) for w in (30, 25, 15, 8)]
HIERARCHIES = [(i % 2, 2 + i % 3) for i in range(max(1, TOTAL // 20))]  # (tier, subclass count)

enums = []
for i in range(6):
    n = rnd.randint(3, 9)
    enums.append(("Kind%d" % i, ["V%d_%d" % (i, j) for j in range(n)]))

classes = []  # dicts: name, tier, parent, abstract, fields
by_tier = {t: [] for t in range(len(TIERS))}
bases = []  # (name, tier, [subclass names])

def field_count():
    return max(2, min(30, int(rnd.lognormvariate(2.2, 0.5))))

cid = 0
def new_class(tier, parent=None, abstract=False):
    global cid
    name = ("Base%d" % cid) if abstract else ("Entity%d" % cid)
    cid += 1
    c = dict(name=name, tier=tier, parent=parent, abstract=abstract, fields=[])
    classes.append(c)
    return c

for tier, subs in HIERARCHIES:
    base = new_class(tier, abstract=True)
    sub_names = [new_class(tier, parent=base["name"])["name"] for _ in range(subs)]
    bases.append((base["name"], tier, sub_names))
for tier, count in enumerate(TIERS):
    existing = sum(1 for c in classes if c["tier"] == tier)
    for _ in range(count - existing):
        new_class(tier)
for c in classes:
    if not c["abstract"]: by_tier[c["tier"]].append(c["name"])

WORDS = "alpha beta gamma delta order customer invoice account session token product price amount status pending active closed region europe north south config value limit timeout retry user admin group role permission cart item shipping address street city zip country phone email note comment description title label".split()

def gen_fields(c):
    tier = c["tier"]
    n = field_count() if not c["abstract"] else rnd.randint(2, 5)
    lower = [x for t in range(tier) for x in by_tier[t]]
    lower_bases = [b for b in bases if b[1] < tier]
    fields = []
    for i in range(n):
        r = rnd.random()
        if tier > 0 and lower and r < 0.14: kind = ("ref", rnd.choice(lower))
        elif tier > 0 and lower and r < 0.21: kind = ("list", rnd.choice(lower))
        elif tier > 0 and lower and r < 0.24: kind = ("map", rnd.choice(lower))
        elif tier > 0 and lower_bases and r < 0.28: kind = ("poly", rnd.choice(lower_bases)[0])
        else:
            kind = (rnd.choices(["int", "long", "double", "boolean", "String", "enum", "Integer", "Long", "int[]", "double[]"],
                [22, 9, 7, 9, 0 if NO_STRINGS else 22, 9, 3, 2, 2, 1])[0], None)
            if kind[0] == "enum": kind = ("enum", rnd.choice(enums)[0])
        fields.append(("f%d_%s" % (i, c["name"].lower()), kind))
    c["fields"] = fields

for c in classes: gen_fields(c)

KEY = "Integer" if NO_STRINGS else "String"
JTYPE = {"int": "int", "long": "long", "double": "double", "boolean": "boolean", "String": "String", "Integer": "Integer",
    "Long": "Long", "int[]": "int[]", "double[]": "double[]"}

def jtype(kind):
    k, t = kind
    if k == "ref" or k == "poly" or k == "enum": return t
    if k == "list": return "List<%s>" % t
    if k == "map": return "Map<%s, %s>" % (KEY, t)
    return JTYPE[k]

def create_expr(name):
    if any(b[0] == name for b in bases): return "%s.createAny(g)" % name
    return "g.obj(%s.class, () -> %s.create(g))" % (name, name)

def init(fname, kind):
    k, t = kind
    if k == "int": return "%s = g.r.nextInt(1000) - 100;" % fname
    if k == "long": return "%s = g.r.nextLong() >>> g.r.nextInt(64);" % fname
    if k == "double": return "%s = g.r.nextDouble() * 1000;" % fname
    if k == "boolean": return "%s = g.r.nextBoolean();" % fname
    if k == "String": return "%s = g.string();" % fname
    if k == "Integer": return "%s = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);" % fname
    if k == "Long": return "%s = g.r.nextInt(10) == 0 ? null : g.r.nextLong();" % fname
    if k == "int[]": return "%s = g.ints();" % fname
    if k == "double[]": return "%s = g.doubles();" % fname
    if k == "enum": return "%s = g.r.nextInt(8) == 0 ? null : g.pick(%s.values());" % (fname, t)
    if k == "ref" or k == "poly": return "%s = g.r.nextInt(7) == 0 ? null : %s;" % (fname, create_expr(t))
    if k == "list":
        return "%s = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (%s != null) for (int i = 0, n = g.size(); i < n; i++) %s.add(%s);" % (
            fname, fname, fname, create_expr(t))
    if k == "map":
        return "%s = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (%s != null) for (int i = 0, n = g.size(); i < n; i++) %s.put(g.key(), %s);" % (
            fname, fname, fname, create_expr(t))

pkgdir = os.path.join(OUT, *PKG.split("."))
os.makedirs(pkgdir, exist_ok=True)
# Remove previously generated files (all but StateBenchmark.java), in case the class count shrank.
for f in os.listdir(pkgdir):
    if f.endswith(".java") and f != "StateBenchmark.java": os.remove(os.path.join(pkgdir, f))
LICENSE = '''/* Copyright (c) 2008-2026, Nathan Sweet
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

// Generated by benchmarks/state-generator.py, do not edit.

'''

def write(name, source):
    open(os.path.join(pkgdir, name + ".java"), "w").write(LICENSE + source)

HDR = "package %s;\n\nimport com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;\nimport java.util.*;\n\n" % PKG

for name, values in enums:
    write(name, "package %s;\n\npublic enum %s { %s }\n" % (PKG, name, ", ".join(values)))

tag_offset = {}
for c in classes:
    # Tags must be unique across the hierarchy: subclasses start after their parent's tags.
    off = len(next(p for p in classes if p["name"] == c["parent"])["fields"]) if c["parent"] else 0
    s = HDR
    s += "public %sclass %s%s {\n" % ("abstract " if c["abstract"] else "", c["name"], (" extends " + c["parent"]) if c["parent"] else "")
    for i, (fname, kind) in enumerate(c["fields"]):
        s += "\t@Tag(%d) %s %s %s;\n" % (off + i, "protected" if c["abstract"] else "private", jtype(kind), fname)
    s += "\n\tpublic %s () {\n\t}\n\n" % c["name"]
    s += "\t%svoid init (Gen g) {\n" % ("" if c["abstract"] else "")
    if c["parent"]: s += "\t\tsuper.init(g);\n"
    for fname, kind in c["fields"]:
        s += "\t\t%s\n" % init(fname, kind)
    s += "\t}\n"
    if c["abstract"]:
        subs = next(b[2] for b in bases if b[0] == c["name"])
        s += "\n\tstatic %s createAny (Gen g) {\n\t\tswitch (g.r.nextInt(%d)) {\n" % (c["name"], len(subs))
        for i, sub in enumerate(subs):
            s += "\t\tcase %d: return g.obj(%s.class, () -> %s.create(g));\n" % (i, sub, sub)
        s += "\t\tdefault: throw new IllegalStateException();\n\t\t}\n\t}\n"
    else:
        s += "\n\tstatic %s create (Gen g) {\n\t\t%s o = new %s();\n\t\to.init(g);\n\t\treturn o;\n\t}\n" % (c["name"], c["name"], c["name"])
    s += "}\n"
    write(c["name"], s)

tops = by_tier[len(TIERS) - 1]
root = HDR + "public class AppState {\n"
root += "\t@Tag(0) private String name;\n\t@Tag(1) private long version;\n"
for i, t in enumerate(tops):
    root += "\t@Tag(%d) private List<%s> list%d;\n" % (i + 3, t, i)
root += "\t@Tag(2) private Map<%s, %s> index;\n\n\tpublic AppState () {\n\t}\n\n" % (KEY, tops[0])
root += "\tpublic static AppState create (long seed, int scale) {\n\t\tGen g = new Gen(seed);\n\t\tAppState s = new AppState();\n"
root += "\t\ts.name = \"state\";\n\t\ts.version = 7;\n"
for i, t in enumerate(tops):
    root += "\t\ts.list%d = new ArrayList<>();\n\t\tfor (int i = 0; i < scale; i++) s.list%d.add(%s.create(g));\n" % (i, i, t)
root += "\t\ts.index = new HashMap<>();\n\t\tfor (%s e : s.list0) s.index.put(g.key(), e);\n" % tops[0]
root += "\t\treturn s;\n\t}\n\n"
root += "\tpublic static Class[] classes () {\n\t\treturn new Class[] {%s};\n\t}\n}\n" % ", ".join(
    "%s.class" % n for n in [e[0] for e in enums] + [c["name"] for c in classes] + ["AppState"])
write("AppState", root)

gen = HDR + '''import java.util.function.Supplier;

/** Deterministic data generator. Every fifth requested object reuses an existing instance of its class (shared references). */
final class Gen {
	static final String[] WORDS = "%s".split(" ");
	final Random r;
	final Map<Class, List<Object>> pool = new HashMap<>();

	Gen (long seed) {
		r = new Random(seed);
	}

	<T> T obj (Class<T> type, Supplier<T> create) {
		List<Object> existing = pool.computeIfAbsent(type, k -> new ArrayList<>());
		if (!existing.isEmpty() && r.nextInt(5) == 0) return (T)existing.get(r.nextInt(existing.size()));
		T o = create.get();
		existing.add(o);
		return o;
	}

	int size () {
		int n = r.nextInt(10);
		return n < 3 ? n : n < 8 ? n - 2 : 4 + r.nextInt(8);
	}

	String string () {
		int n = r.nextInt(10);
		if (n == 0) return null;
		if (n == 1) return "";
		StringBuilder b = new StringBuilder(WORDS[r.nextInt(WORDS.length)]);
		for (int i = 0, c = r.nextInt(4); i < c; i++)
			b.append(' ').append(WORDS[r.nextInt(WORDS.length)]);
		if (r.nextInt(20) == 0) b.append(" \\u00fcber\\u00e9");
		return b.toString();
	}

	%s key () {
		return %s;
	}

	int[] ints () {
		if (r.nextInt(5) == 0) return null;
		int[] a = new int[r.nextInt(16)];
		for (int i = 0; i < a.length; i++)
			a[i] = r.nextInt(5000);
		return a;
	}

	double[] doubles () {
		if (r.nextInt(5) == 0) return null;
		double[] a = new double[r.nextInt(8)];
		for (int i = 0; i < a.length; i++)
			a[i] = r.nextDouble();
		return a;
	}

	<T> T pick (T[] values) {
		return values[r.nextInt(values.length)];
	}
}
''' % (" ".join(WORDS), KEY, "r.nextInt(100000)" if NO_STRINGS else 'WORDS[r.nextInt(WORDS.length)] + "-" + r.nextInt(100000)')
write("Gen", gen)

nf = [len(c["fields"]) for c in classes if not c["abstract"]]
print("classes", len(classes), "concrete", len(nf), "enums", len(enums), "fields min/avg/max", min(nf), sum(nf) / len(nf), max(nf))
