package com.example.mytodoapp

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: View
    private lateinit var ivBackground: ImageView
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var rvTodoItems: RecyclerView
    private lateinit var etTodoTitle: EditText
    private lateinit var btnAddTodo: FloatingActionButton
    private lateinit var priorityChipGroup: ChipGroup
    private lateinit var btnSetStartTime: MaterialButton
    private lateinit var btnSetEndTime: MaterialButton

    private val taskList = mutableListOf<Task>()
    private var nextId = 1L

    private var startCalendar: Calendar? = null
    private var endCalendar: Calendar? = null

    private val importResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == Activity.RESULT_OK) { result.data?.data?.also { uri -> importTasksFromUri(uri) } } }
    private val exportResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == Activity.RESULT_OK) { result.data?.data?.also { uri -> exportTasksToUri(uri) } } }
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                saveSettings(imageUri = uri.toString())
                applyBackgroundImage(uri)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadAndApplySettings()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "我的待办事项"

        setupRecyclerView()
        loadTasks()

        setupClickListeners()
        setupAdapterListeners()
    }

    private fun initViews() {
        rootLayout = findViewById(R.id.root_layout)
        ivBackground = findViewById(R.id.iv_background)
        rvTodoItems = findViewById(R.id.rvTodoItems)
        etTodoTitle = findViewById(R.id.etTodoTitle)
        btnAddTodo = findViewById(R.id.btnAddTodo)
        priorityChipGroup = findViewById(R.id.priorityChipGroup)
        btnSetStartTime = findViewById(R.id.btnSetStartTime)
        btnSetEndTime = findViewById(R.id.btnSetEndTime)
    }

    private fun setupRecyclerView() { todoAdapter = TodoAdapter(taskList); rvTodoItems.adapter = todoAdapter; rvTodoItems.layoutManager = LinearLayoutManager(this) }
    private fun setupClickListeners() { btnSetStartTime.setOnClickListener { showDateTimePicker { calendar -> startCalendar = calendar; val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()); btnSetStartTime.text = sdf.format(calendar.time) } }; btnSetEndTime.setOnClickListener { showDateTimePicker { calendar -> endCalendar = calendar; val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()); btnSetEndTime.text = sdf.format(calendar.time) } }; btnAddTodo.setOnClickListener { addTask() } }
    private fun addTask() { val todoTitle = etTodoTitle.text.toString(); if (todoTitle.isNotBlank()) { val selectedPriority = when (priorityChipGroup.checkedChipId) { R.id.chipHigh -> Priority.HIGH; R.id.chipLow -> Priority.LOW; else -> Priority.MEDIUM }; val task = Task(id = nextId++, text = todoTitle, startTime = startCalendar?.timeInMillis, endTime = endCalendar?.timeInMillis, priority = selectedPriority); taskList.add(0, task); todoAdapter.notifyItemInserted(0); rvTodoItems.scrollToPosition(0); resetInputFields() } }
    private fun resetInputFields() { etTodoTitle.text.clear(); priorityChipGroup.check(R.id.chipMedium); startCalendar = null; endCalendar = null; btnSetStartTime.text = "开始时间"; btnSetEndTime.text = "截止时间" }
    private fun showDateTimePicker(onDateTimeSet: (Calendar) -> Unit) { val calendar = Calendar.getInstance(); DatePickerDialog(this, { _, year, month, dayOfMonth -> TimePickerDialog(this, { _, hourOfDay, minute -> val selectedDateTime = Calendar.getInstance(); selectedDateTime.set(year, month, dayOfMonth, hourOfDay, minute); onDateTimeSet(selectedDateTime) }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show() }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show() }
    private fun setupAdapterListeners() { todoAdapter.onDeleteClick = { task -> val position = taskList.indexOf(task); if (position != -1) { taskList.removeAt(position); todoAdapter.notifyItemRemoved(position) } }; todoAdapter.onTaskCheckedChange = { task, _ -> todoAdapter.notifyItemChanged(taskList.indexOf(task)) } }

    private fun saveTasks() { val sharedPrefs = getSharedPreferences("TodoAppTasks", Context.MODE_PRIVATE); val editor = sharedPrefs.edit(); val gson = Gson(); val json = gson.toJson(taskList); editor.putString("taskListJson", json); editor.putLong("nextId", nextId); editor.apply() }
    private fun loadTasks() { val sharedPrefs = getSharedPreferences("TodoAppTasks", Context.MODE_PRIVATE); val gson = Gson(); val json = sharedPrefs.getString("taskListJson", null); val type = object : TypeToken<MutableList<Task>>() {}.type; if (json != null) { val loadedTasks: MutableList<Task> = gson.fromJson(json, type); taskList.clear(); taskList.addAll(loadedTasks) }; nextId = sharedPrefs.getLong("nextId", 1L); todoAdapter.notifyDataSetChanged() }
    override fun onStop() { super.onStop(); saveTasks() }

    private fun saveSettings(bgColor: Int? = null, imageUri: String? = null) {
        val sharedPrefs = getSharedPreferences("TodoAppSettings", Context.MODE_PRIVATE).edit()

        if (imageUri != null) {
            sharedPrefs.putString("backgroundImageUri", imageUri)
            sharedPrefs.remove("backgroundColor")
        } else if (bgColor != null) {
            sharedPrefs.putInt("backgroundColor", bgColor)
            sharedPrefs.remove("backgroundImageUri")
        } else {
            // 当两个参数都为null时，表示恢复默认，清除两个设置
            sharedPrefs.remove("backgroundImageUri")
            sharedPrefs.remove("backgroundColor")
        }

        sharedPrefs.apply()
    }

    private fun loadAndApplySettings() {
        val sharedPrefs = getSharedPreferences("TodoAppSettings", Context.MODE_PRIVATE)
        val imageUriString = sharedPrefs.getString("backgroundImageUri", null)
        val bgColor = sharedPrefs.getInt("backgroundColor", -1)

        if (imageUriString != null) {
            applyBackgroundImage(Uri.parse(imageUriString))
        } else if (bgColor != -1) {
            applyBackgroundColor(bgColor)
        } else {
            // 如果两者都未设置，则XML中的默认src会显示
            // 确保根布局是透明的，以显示下面的ImageView
            rootLayout.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun applyBackgroundColor(color: Int) {
        ivBackground.setImageDrawable(null)
        rootLayout.setBackgroundColor(color)
    }

    private fun applyBackgroundImage(uri: Uri) {
        rootLayout.setBackgroundColor(Color.TRANSPARENT)
        ivBackground.setImageURI(uri)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean { menuInflater.inflate(R.menu.main_menu, menu); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> { launchExport(); true }
            R.id.action_import -> { launchImport(); true }
            R.id.action_change_background -> { showBackgroundPickerDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showBackgroundPickerDialog() {
        val items = arrayOf(
            getString(R.string.select_from_gallery),
            "恢复默认背景", "薄荷", "薰衣草", "蜜桃"
        )
        val colors = intArrayOf(
            0, // Placeholder
            0, // Placeholder for default
            ContextCompat.getColor(this, R.color.bg_mint),
            ContextCompat.getColor(this, R.color.bg_lavender),
            ContextCompat.getColor(this, R.color.bg_peach)
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.change_background_dialog_title))
            .setItems(items) { dialog, which ->
                when (which) {
                    0 -> { // 从相册选择
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "image/*"
                        }
                        imagePickerLauncher.launch(intent)
                    }
                    1 -> { // 恢复默认
                        saveSettings(imageUri = null, bgColor = null)
                        ivBackground.setImageResource(R.drawable.my_background_image) // 重新设置你的默认图片
                        rootLayout.setBackgroundColor(Color.TRANSPARENT)
                    }
                    else -> { // 选择纯色
                        applyBackgroundColor(colors[which])
                        saveSettings(bgColor = colors[which], imageUri = null)
                    }
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun launchExport() { val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, "MyTodos_${System.currentTimeMillis()}.json") }; exportResultLauncher.launch(intent) }
    private fun launchImport() { val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }; importResultLauncher.launch(intent) }
    private fun exportTasksToUri(uri: Uri) { try { val gson = Gson(); val jsonString = gson.toJson(taskList); contentResolver.openFileDescriptor(uri, "w")?.use { FileOutputStream(it.fileDescriptor).use { fos -> fos.write(jsonString.toByteArray()) } }; Toast.makeText(this, "导出成功!", Toast.LENGTH_SHORT).show() } catch (e: Exception) { Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_LONG).show() } }
    private fun importTasksFromUri(uri: Uri) { try { val stringBuilder = StringBuilder(); contentResolver.openInputStream(uri)?.use { inputStream -> BufferedReader(InputStreamReader(inputStream)).use { reader -> var line: String? = reader.readLine(); while (line != null) { stringBuilder.append(line); line = reader.readLine() } } }; val gson = Gson(); val type = object : TypeToken<List<Task>>() {}.type; val importedTasks: List<Task> = gson.fromJson(stringBuilder.toString(), type); taskList.clear(); taskList.addAll(importedTasks); if (taskList.isNotEmpty()) { nextId = (taskList.maxOfOrNull { it.id } ?: 0L) + 1 }; todoAdapter.notifyDataSetChanged(); Toast.makeText(this, "导入成功!", Toast.LENGTH_SHORT).show() } catch (e: Exception) { Toast.makeText(this, "导入失败: ${e.message}", Toast.LENGTH_LONG).show() } }
}
