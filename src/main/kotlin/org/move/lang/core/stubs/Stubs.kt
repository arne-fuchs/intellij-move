package org.move.lang.core.stubs

import com.intellij.psi.stubs.*
import com.intellij.util.BitUtil
import com.intellij.util.io.DataInputOutputUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.impl.*
import org.move.stdext.makeBitMask

interface MvNamedStub {
    val name: String?
}

interface MvAttributeOwnerStub {
    val hasAttrs: Boolean

    companion object {
        val ATTRS_MASK: Int = makeBitMask(0)
        const val USED_BITS: Int = 1

        fun extractFlags(element: MvDocAndAttributeOwner): Int =
            extractFlags(element.queryAttributes)

        fun extractFlags(query: QueryAttributes): Int {
            val hasAttrs = query.attrItems.iterator().hasNext()

            var flags = 0
            flags = BitUtil.set(flags, ATTRS_MASK, hasAttrs)
            return flags
        }
    }
}

abstract class MvAttributeOwnerStubBase<T : MvElement>(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>
) : StubBase<T>(parent, elementType),
    MvAttributeOwnerStub {

    override val hasAttrs: Boolean
        get() = BitUtil.isSet(flags, MvAttributeOwnerStub.ATTRS_MASK)

    protected abstract val flags: Int
}

class MvModuleStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?,
    override val flags: Int
) : MvAttributeOwnerStubBase<MvModule>(parent, elementType), MvNamedStub {

    val isTestOnly: Boolean get() = BitUtil.isSet(flags, TEST_ONLY_MASK)

    object Type : MvStubElementType<MvModuleStub, MvModule>("MODULE") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvModuleStub(
                parentStub,
                this,
                dataStream.readNameAsString(),
                dataStream.readInt()
            )

        override fun serialize(stub: MvModuleStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
                writeInt(stub.flags)
            }

        override fun createPsi(stub: MvModuleStub): MvModule =
            MvModuleImpl(stub, this)

        override fun createStub(psi: MvModule, parentStub: StubElement<*>?): MvModuleStub {
            val attrs = QueryAttributes(psi.attrList.asSequence())

            var flags = MvAttributeOwnerStub.extractFlags(attrs)
            flags = BitUtil.set(flags, TEST_ONLY_MASK, attrs.isTestOnly)

            return MvModuleStub(parentStub, this, psi.name, flags)
        }

        override fun indexStub(stub: MvModuleStub, sink: IndexSink) = sink.indexModuleStub(stub)
    }

    companion object {
        private val TEST_ONLY_MASK: Int = makeBitMask(MvAttributeOwnerStub.USED_BITS + 0)
    }
}

class MvFunctionStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?,
    override val flags: Int
) : MvAttributeOwnerStubBase<MvFunction>(parent, elementType), MvNamedStub {

    val isTestOnly: Boolean get() = BitUtil.isSet(flags, TEST_ONLY_MASK)
    val isTest: Boolean get() = BitUtil.isSet(flags, TEST_MASK)

    object Type : MvStubElementType<MvFunctionStub, MvFunction>("FUNCTION") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvFunctionStub(
                parentStub,
                this,
                dataStream.readNameAsString(),
                dataStream.readInt()
            )

        override fun serialize(stub: MvFunctionStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
                writeInt(stub.flags)
            }

        override fun createPsi(stub: MvFunctionStub): MvFunction =
            MvFunctionImpl(stub, this)

        override fun createStub(psi: MvFunction, parentStub: StubElement<*>?): MvFunctionStub {
            val attrs = QueryAttributes(psi.attrList.asSequence())

            var flags = MvAttributeOwnerStub.extractFlags(attrs)
            flags = BitUtil.set(flags, TEST_ONLY_MASK, attrs.isTestOnly)
            flags = BitUtil.set(flags, TEST_MASK, attrs.isTest)

            return MvFunctionStub(parentStub, this, psi.name, flags)
        }

        override fun indexStub(stub: MvFunctionStub, sink: IndexSink) = sink.indexFunctionStub(stub)
    }

    companion object {
        private val TEST_ONLY_MASK: Int = makeBitMask(MvAttributeOwnerStub.USED_BITS + 0)
        private val TEST_MASK: Int = makeBitMask(MvAttributeOwnerStub.USED_BITS + 1)
    }
}

class MvSpecFunctionStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?,
) : StubBase<MvSpecFunction>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvSpecFunctionStub, MvSpecFunction>("SPEC_FUNCTION") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvSpecFunctionStub(
                parentStub,
                this,
                dataStream.readNameAsString(),
            )

        override fun serialize(stub: MvSpecFunctionStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
            }

        override fun createPsi(stub: MvSpecFunctionStub): MvSpecFunction =
            MvSpecFunctionImpl(stub, this)

        override fun createStub(psi: MvSpecFunction, parentStub: StubElement<*>?): MvSpecFunctionStub {
            return MvSpecFunctionStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: MvSpecFunctionStub, sink: IndexSink) = sink.indexSpecFunctionStub(stub)
    }
}

class MvStructStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?
) : StubBase<MvStruct>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvStructStub, MvStruct>("STRUCT") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvStructStub(
                parentStub,
                this,
                dataStream.readNameAsString()
            )

        override fun serialize(stub: MvStructStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
            }

        override fun createPsi(stub: MvStructStub): MvStruct =
            MvStructImpl(stub, this)

        override fun createStub(psi: MvStruct, parentStub: StubElement<*>?): MvStructStub {
            return MvStructStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: MvStructStub, sink: IndexSink) = sink.indexStructStub(stub)
    }
}

class MvSchemaStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?
) : StubBase<MvSchema>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvSchemaStub, MvSchema>("SCHEMA") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvSchemaStub(
                parentStub,
                this,
                dataStream.readNameAsString()
            )

        override fun serialize(stub: MvSchemaStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
            }

        override fun createPsi(stub: MvSchemaStub): MvSchema =
            MvSchemaImpl(stub, this)

        override fun createStub(psi: MvSchema, parentStub: StubElement<*>?): MvSchemaStub {
            return MvSchemaStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: MvSchemaStub, sink: IndexSink) = sink.indexSchemaStub(stub)
    }
}

class MvConstStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?
) : StubBase<MvConst>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvConstStub, MvConst>("CONST") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvConstStub(
                parentStub,
                this,
                dataStream.readNameAsString()
            )

        override fun serialize(stub: MvConstStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
            }

        override fun createPsi(stub: MvConstStub): MvConst =
            MvConstImpl(stub, this)

        override fun createStub(psi: MvConst, parentStub: StubElement<*>?): MvConstStub {
            return MvConstStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: MvConstStub, sink: IndexSink) = sink.indexConstStub(stub)
    }
}

class MvModuleSpecStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    val moduleName: String?,
) : StubBase<MvModuleSpec>(parent, elementType) {

    object Type : MvStubElementType<MvModuleSpecStub, MvModuleSpec>("MODULE_SPEC") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvModuleSpecStub(
                parentStub,
                this,
                dataStream.readUTFFastAsNullable()
            )

        override fun serialize(stub: MvModuleSpecStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeUTFFastAsNullable(stub.moduleName)
            }

        override fun createPsi(stub: MvModuleSpecStub): MvModuleSpec =
            MvModuleSpecImpl(stub, this)

        override fun createStub(psi: MvModuleSpec, parentStub: StubElement<*>?): MvModuleSpecStub {
            return MvModuleSpecStub(parentStub, this, psi.fqModuleRef?.stubText())
        }

        override fun indexStub(stub: MvModuleSpecStub, sink: IndexSink) = sink.indexModuleSpecStub(stub)
    }
}


fun factory(name: String): MvStubElementType<*, *> = when (name) {
    "MODULE" -> MvModuleStub.Type
    "FUNCTION" -> MvFunctionStub.Type
    "SPEC_FUNCTION" -> MvSpecFunctionStub.Type
    "STRUCT" -> MvStructStub.Type
    "SCHEMA" -> MvSchemaStub.Type
    "CONST" -> MvConstStub.Type
    "MODULE_SPEC" -> MvModuleSpecStub.Type

    else -> error("Unknown element $name")
}

private fun StubInputStream.readNameAsString(): String? = readName()?.string
private fun StubInputStream.readUTFFastAsNullable(): String? =
    DataInputOutputUtil.readNullable(this, this::readUTFFast)

private fun StubOutputStream.writeUTFFastAsNullable(value: String?) =
    DataInputOutputUtil.writeNullable(this, value, this::writeUTFFast)

private fun StubOutputStream.writeLongAsNullable(value: Long?) =
    DataInputOutputUtil.writeNullable(this, value, this::writeLong)

private fun StubInputStream.readLongAsNullable(): Long? = DataInputOutputUtil.readNullable(this, this::readLong)

private fun StubOutputStream.writeDoubleAsNullable(value: Double?) =
    DataInputOutputUtil.writeNullable(this, value, this::writeDouble)

private fun StubInputStream.readDoubleAsNullable(): Double? =
    DataInputOutputUtil.readNullable(this, this::readDouble)
