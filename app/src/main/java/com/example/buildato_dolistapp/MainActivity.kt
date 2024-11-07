package com.example.buildato_dolistapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var taskList: MutableList<Task>
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdaptor
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editText: EditText
    private lateinit var editText2: EditText
    private lateinit var addButton: Button

    private var isEditMode = false // Flag to indicate if we're in edit mode
    private var editingTaskIndex: Int = -1 // Holds the index of the task being edited

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("tasks", Context.MODE_PRIVATE)
        editText = findViewById(R.id.editTxt)
        editText2 = findViewById(R.id.editTxt2)
        addButton = findViewById(R.id.btn)
        recyclerView = findViewById(R.id.Rv)

        // Retrieve and sort tasks by id
        taskList = retrieveTasks()

        taskAdapter = TaskAdaptor(taskList, object : TaskAdaptor.TaskClickListener {
            override fun onEditClick(position: Int) {
                // Populate edit texts with task details and switch to edit mode
                editText.setText(taskList[position].title)
                editText2.setText(taskList[position].description)
                isEditMode = true
                editingTaskIndex = position
                addButton.text = "Update" // Change button text to "Update"
            }

            override fun onDeleteClick(position: Int) {
                taskList.removeAt(position)
                taskAdapter.notifyItemRemoved(position)
                saveTasks(taskList)
            }
        })

        recyclerView.adapter = taskAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        addButton.setOnClickListener {
            val taskText = editText.text.toString()
            val taskDescription = editText2.text.toString()
            if (taskText.isNotEmpty() || taskDescription.isNotEmpty()) {
                if (isEditMode) {
                    // Update existing task
                    taskList[editingTaskIndex].title = taskText
                    taskList[editingTaskIndex].description = taskDescription
                    taskAdapter.notifyItemChanged(editingTaskIndex)
                    isEditMode = false
                    addButton.text = "Add" // Reset button text to "Add"
                } else {
                    // Add new task with a unique id based on the current list size
                    val task = Task(taskList.size + 1, taskText, taskDescription, false)
                    taskList.add(task)
                    taskAdapter.notifyItemInserted(taskList.size - 1)
                }
                saveTasks(taskList)
                editText.text.clear()
                editText2.text.clear()
            } else {
                Toast.makeText(this, "Task title can't be empty", Toast.LENGTH_SHORT).show()
            }
        }

        // Attach ItemTouchHelper for swipe actions
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                taskList.removeAt(position)
                taskAdapter.notifyItemRemoved(position)
                saveTasks(taskList)
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun saveTasks(taskList: MutableList<Task>) {
        val editor = sharedPreferences.edit()
        val taskSet = HashSet<String>()
        // Include id, title, and description in each task string to save in SharedPreferences
        taskList.forEach { taskSet.add("${it.id}|${it.title}|${it.description}") }
        editor.putStringSet("tasks", taskSet)
        editor.apply()
    }

    private fun retrieveTasks(): MutableList<Task> {
        val tasks = sharedPreferences.getStringSet("tasks", HashSet()) ?: HashSet()
        return tasks.map {
            val parts = it.split("|")
            val id = parts[0].toIntOrNull() ?: 0 // Retrieve the id from the saved data
            val title = parts.getOrNull(1) ?: ""
            val description = parts.getOrNull(2) ?: ""
            Task(id, title, description, false)
        }.sortedBy { it.id } // Sort by id after mapping
            .toMutableList()
    }
}

