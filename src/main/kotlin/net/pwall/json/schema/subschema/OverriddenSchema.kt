package net.pwall.json.schema.subschema

import net.pwall.json.JSONMapping
import net.pwall.json.JSONValue
import net.pwall.json.pointer.JSONPointer
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry
import net.pwall.json.schema.output.BasicOutput
import net.pwall.json.schema.output.DetailedOutput
import java.net.URI

/**
 * Hack for overriding properties belonging to an interface in [existingInterfaces]. Find a better way to do this.
 */
class OverriddenSchema(uri: URI?, location: JSONPointer, val properties: List<String>) :
    JSONSchema.SubSchema(uri, location) {

    override fun childLocation(pointer: JSONPointer): JSONPointer = pointer.child("overridden")

    override fun validate(json: JSONValue?, instanceLocation: JSONPointer): Boolean {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONMapping<*>)
            return true
        for (property in properties)
            if (!instance.containsKey(property))
                return false
        return true
    }

    override fun validateBasic(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            BasicOutput {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONMapping<*>)
            return BasicOutput.trueOutput
        val errors = mutableListOf<BasicErrorEntry>()
        properties.forEachIndexed { i, property ->
            if (!instance.containsKey(property))
                errors.add(createBasicErrorEntry(relativeLocation.child(i), instanceLocation,
                    "Overridden property \"$property\" not found"))
        }
        if (errors.isEmpty())
            return BasicOutput.trueOutput
        return BasicOutput(false, errors)
    }

    override fun validateDetailed(relativeLocation: JSONPointer, json: JSONValue?, instanceLocation: JSONPointer):
            DetailedOutput {
        val instance = instanceLocation.eval(json)
        if (instance !is JSONMapping<*>)
            return createAnnotation(relativeLocation, instanceLocation, "Value is not an object")
        val errors = mutableListOf<DetailedOutput>()
        properties.forEachIndexed { i, property ->
            if (!instance.containsKey(property))
                errors.add(createError(relativeLocation.child(i), instanceLocation,
                    "Overridden property \"$property\" not found"))
        }
        return when (errors.size) {
            0 -> createAnnotation(relativeLocation, instanceLocation, "All overridden properties found")
            1 -> errors[0]
            else -> createError(relativeLocation, instanceLocation, "Overridden property error", errors = errors)
        }
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is OverriddenSchema && super.equals(other) && properties == other.properties

    override fun hashCode(): Int = super.hashCode() xor properties.hashCode()

}