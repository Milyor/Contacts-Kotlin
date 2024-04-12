import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*
import kotlinx.datetime.Clock
import java.io.File
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import kotlinx.datetime.Instant

open class Contacts(
    open var name: String,
    open var phone: String,
    open val created: Instant = Clock.System.now(),
    open var timeEdited: Instant = Clock.System.now()
)
data class Person(
    override var name: String,
    override var phone: String,
    var surname: String,
    var gender: String,
    var birthDate: String
) : Contacts(name, phone)

class Organization(
     override var name: String,
     override var phone: String,
     var address: String,
    ) : Contacts(name, phone)

class InstantAdapter {
    @ToJson
    fun toJson(instant: Instant): String {
        return instant.toString()
    }

    @FromJson
    fun fromJson(value: String): Instant {
        return Instant.parse(value)
    }
}

class ContactBook {

    private val myFile = File("phonebook.json")
    private val phoneBook = mutableListOf<Contacts>()

    // Running the adapters to convert my phoneBook list in to a json

    private val adapterFactory = PolymorphicJsonAdapterFactory
        .of(Contacts::class.java, "type")
        .withSubtype(Person::class.java, "Person")
        .withSubtype(Organization::class.java,"Organization")

     private val moshi = Moshi.Builder()
        .add(adapterFactory)
        .add(InstantAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val type = Types.newParameterizedType(List::class.java, Contacts::class.java)
    private val contactsListAdapter : JsonAdapter<List<Contacts>> = moshi.adapter(type)

    // Working on file creating and management to save and load the Json file or create it.

    private fun saveContacts() {
        val json = contactsListAdapter.toJson(phoneBook)
        myFile.writeText(json)
    }

    private fun loadContacts(){
        if (myFile.exists()){
            val json = myFile.readText()
            try {
                val loadedContacts = contactsListAdapter.fromJson(json)
                if (loadedContacts != null){
                    phoneBook.clear()
                    phoneBook.addAll(loadedContacts)
                } else {
                    println("Phonebook file not found")
                }
            } catch (e: JsonDataException) {
                println("Error deserializing contacts: ${e.message}")
            } catch (e: Exception) {
                println("General error loading contacts: ${e.message}")
            }
        }
    }

    fun listContacts() {
        displayContactsWithIndices()
        print("\n[list] Enter action ([number], back):")
        when(val input = readln()) {
            "back" -> return
             else -> {
                 val index = input.toInt() - 1
                 display(index)
                 val contact = getContactAtIndices(index)!!
                 recordMenu(contact, index)
             }
        }
    }


    fun search () {
        while (true){
            print("Enter search query: ")
            val search = readln()
            // Regex to match contacts, and find them by name or last name if it's a person
            val regex = Regex(".*$search.*", RegexOption.IGNORE_CASE)
            // Creating a temporary index for display simplicity
            var position = 1
            val resultMap = mutableMapOf<Contacts, Int>()
            for ((index, contact) in phoneBook.withIndex()) {
                when (contact) {
                    is Person -> {
                        if (contact.name.matches(regex) || contact.surname.matches(regex) || contact.phone.matches(regex)) {
                            println("$position. ${contact.name} ${contact.surname}")
                            resultMap[contact] = index
                        }
                    }
                    is Organization -> {
                        if (contact.name.matches(regex) || contact.phone.matches(regex)) {
                            println("$position. ${contact.name}")
                            resultMap[contact] = index
                        }
                    }
                }
                position++
            }
            print("[search] Enter action ([number], back. again): ")
            when (val input = readln()) {
                "back" -> {
                    resultMap.clear()
                    return
                }
                "again" -> {
                    resultMap.clear()
                    continue
                }
                // Figure out why is resultMap out of reach
                else -> {
                    val contact = resultMap.keys.toList()[input.toInt() - 1]
                    val originalIndex = resultMap[contact]!!//Get the contact
                    displayContactsDetails(contact)
                    recordMenu(contact, originalIndex)
                    return
                }
            }
        }
    }
    private fun recordMenu (contact: Contacts, index: Int) {
        print("[record] Enter action (edit, delete, menu): ")
        when (val newInput = readln()){
            "edit" -> edit(contact, index)
            // Need to modify remove to work with the map, and take a contact type in.
            "delete" -> remove(index)
            "menu" -> return
        }
    }
    fun addContact() {

        while (true) {
            print("Enter the type (person, organization): ")
            when (val contactType = readln()) {
                "person" -> {
                    print("Enter the name: ")
                    val name = readln()
                    print("Enter the surname: ")
                    val surname = readln()
                    print("Enter the birth date: ")
                    var birthDate = readln()
                    if (birthDate.isEmpty()) {
                        println("Bad birth date!")
                        birthDate = "[no data]"
                    }
                    print("Enter the gender (M, F): ")
                    var gender = readln().uppercase()
                    // Gender not working properly displaying Bad gender when it shouldn't
                    if (gender.isEmpty()) {
                        println("Bad gender!")
                        gender = "[no data]"
                    }
                    print("Enter the number: ")
                    var phone = readln()
                    if (!isValidNumber(phone)) {
                        println("Wrong number format!")
                        phone = "[no number]"
                    }
                    val person = Person(name, phone, surname, gender, birthDate)
                    phoneBook.add(person)
                    break
                }

                "organization" -> {
                    print("Organization name: ")
                    val name = readln()
                    print("Enter the address: ")
                    val address = readln()
                    print("Enter the phone number: ")
                    var phone = readln()
                    if (!isValidNumber(phone)) {
                        println("Wrong number format!")
                        phone = "[no number]"
                    }
                    val organization = Organization(name, phone, address)
                    phoneBook.add((organization))
                    break
                }

                else -> {
                    println("Wrong input")
                    continue
                }
            }
        }
        saveContacts()
        println("The record added.")
    }
    private fun isValidNumber(number: String): Boolean {
        val first = "(\\(\\w+\\)([- ]\\w{2,})*)"
        val second = "(\\w+[- ]\\(\\w{2,}\\)([- ]\\w{2,})*)"
        val third = "(\\w+[- ]\\w{2,}([- ]\\w{2,})*)"
        val regex1 = Regex("\\+?(\\w+|$first|$second|$third)")

        return number.matches(regex1)
    }

    private fun edit(contact: Contacts, index: Int) {
        when (contact) {
            is Person -> {
                print("Select a field (name, surname, birth, gender, number):")
                when (val field = readln().lowercase()) {
                    "name" -> {
                        print("Enter name: ")
                        contact.name = readln()
                    }

                    "surname" -> {
                        print("Enter surname: ")
                        contact.surname = readln()
                    }

                    "number" -> {
                        print("Enter the number: ")
                        var phone = readln()
                        if (!isValidNumber(phone)) {
                            println("Wrong number format!")
                            phone = "[no number]"
                        }
                        contact.phone = phone
                    }

                    "birth" -> {
                        print("Enter the birth date: ")
                        contact.birthDate = readln()
                    }

                    "gender" -> {
                        print("Enter the gender (M, F): ")
                        contact.gender = readln()
                    }
                }
                contact.timeEdited = Clock.System.now()
            }

            is Organization -> {
                println("Select a field (address, number):")
                when (val field = readln().lowercase()) {
                    "address" -> {
                        print("Enter address: ")
                        contact.address = readln()
                    }

                    "number" -> {
                        print("Enter number: ")
                        var phone = readln()
                        if (!isValidNumber(phone)) {
                            println("Wrong number format!")
                            phone = "[no number]"
                        }
                        contact.phone = phone
                    }
                }
                contact.timeEdited = Clock.System.now()
                phoneBook[index] = contact
            }
        }
        saveContacts()
        println("Saved")
    }

    private fun display(index: Int){
        while (true){
            try {
                val contact = getContactAtIndices(index)
                // If it's a person print it as it a person and if not print it as Organization.
                if (contact != null) {
                    displayContactsDetails(contact)
                    break
                }
            } catch (e: NumberFormatException) {
                println("invalid input. Please enter a number.")
            }
        }
    }

    private fun remove(index: Int) {
        phoneBook.removeAt(index)
        println("The record removed!")
    }

    private fun displayContactsWithIndices() {
        if (phoneBook.isEmpty()){
            println("The Phone Book has 0 records.")
            return
        }
        var position = 1
        for (contact in phoneBook) {
            when (contact) {
                is Person -> println("$position- ${contact.name} ${contact.surname}")
                is Organization -> println("$position- ${contact.name}")
            }
            position++
        }
    }

    private fun getContactAtIndices(index: Int): Contacts? {
        if (index in phoneBook.indices) {
            return phoneBook[index]
        } else {
            println("Invalid Index.")
            return null
        }
    }

    private fun displayContactsDetails(contact: Contacts) {
        when (contact) {
            is Person -> println(
                "Name: ${contact.name}\n" +
                        "Surname: ${contact.surname}\n" +
                        "Birth date: ${contact.birthDate}\n" +
                        "Gender: ${contact.gender}\n" +
                        "Number: ${contact.phone}\n" +
                        "Time created: ${contact.created}\n" +
                        "Time last edit: ${contact.timeEdited}\n"
            )

            is Organization -> println(
                "Organization name: ${contact.name}\n" +
                        "Address: ${contact.address}\n" +
                        "Number: ${contact.phone}\n" +
                        "Time created: ${contact.created}\n" +
                        "Time last edit: ${contact.timeEdited}\n"
            )
        }
    }
    fun count(){
        if (phoneBook.isEmpty()){
            println("The Phone Book has 0 records.")
            return
        } else {
            println("The Phone Book has ${phoneBook.size} records.")
        }
    }
    // initializing and creating the file if it doesn't exist and loading contacts
    init {
        if (!myFile.exists()) {
            myFile.createNewFile()
            myFile.writeText("[]")
        }
        loadContacts()
    }
}

fun main() {
    val contactBook = ContactBook()
    while (true) {
        print("\n[menu] Enter action (add, list, search, count, exit):")
        when(val action = readln()){
            "add" -> contactBook.addContact()
            "search" -> contactBook.search()
            "list" -> contactBook.listContacts()
            "count" -> contactBook.count()
            "exit" -> return
        }

    }
}