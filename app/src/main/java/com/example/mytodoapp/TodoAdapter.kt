package com.example.mytodoapp

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodoAdapter(
    private val tasks: MutableList<Task>
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    var onDeleteClick: ((Task) -> Unit)? = null
    var onTaskCheckedChange: ((Task, Boolean) -> Unit)? = null

    class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTodoTitle: TextView = itemView.findViewById(R.id.tvTodoTitle)
        val cbDone: CheckBox = itemView.findViewById(R.id.cbDone)
        val ivDelete: ImageButton = itemView.findViewById(R.id.ivDelete)
        val priorityIndicator: View = itemView.findViewById(R.id.priorityIndicator)
        val tvDateTimeRange: TextView = itemView.findViewById(R.id.tvDateTimeRange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val currentTask = tasks[position]
        holder.tvTodoTitle.text = currentTask.text
        holder.cbDone.isChecked = currentTask.isCompleted

        val priorityColor = ContextCompat.getColor(holder.itemView.context, currentTask.priority.colorResId)
        holder.priorityIndicator.setBackgroundColor(priorityColor)

        holder.tvDateTimeRange.text = formatDateTimeRange(currentTask.startTime, currentTask.endTime)

        updateTaskAppearance(holder, currentTask)

        holder.cbDone.setOnCheckedChangeListener(null)
        holder.cbDone.setOnCheckedChangeListener { _, isChecked ->
            currentTask.isCompleted = isChecked // 更新状态
            updateTaskAppearance(holder, currentTask) // 更新外观
            onTaskCheckedChange?.invoke(currentTask, isChecked)
        }

        holder.ivDelete.setOnClickListener {
            onDeleteClick?.invoke(currentTask)
        }
    }

    private fun updateTaskAppearance(holder: TodoViewHolder, task: Task) {
        val context = holder.itemView.context
        val isOverdue = task.endTime != null && System.currentTimeMillis() > task.endTime!! && !task.isCompleted

        // 根据不同状态设置字体颜色和删除线
        if (task.isCompleted) {
            // 已完成：灰色 + 删除线
            setPaintFlags(holder.tvTodoTitle, holder.tvDateTimeRange, true)
            setTextColor(holder.tvTodoTitle, holder.tvDateTimeRange, Color.GRAY)
        } else if (isOverdue) {
            // 逾期未完成：红色
            setPaintFlags(holder.tvTodoTitle, holder.tvDateTimeRange, false)
            setTextColor(holder.tvTodoTitle, holder.tvDateTimeRange, ContextCompat.getColor(context, R.color.task_overdue_text_color))
        } else {
            // 正常未完成：默认颜色
            setPaintFlags(holder.tvTodoTitle, holder.tvDateTimeRange, false)
            setTextColor(holder.tvTodoTitle, Color.BLACK)
            setTextColor(holder.tvDateTimeRange, Color.DKGRAY)
        }
    }

    private fun setPaintFlags(title: TextView, dateTime: TextView, addStrike: Boolean) {
        if (addStrike) {
            title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            dateTime.paintFlags = dateTime.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            title.paintFlags = title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            dateTime.paintFlags = dateTime.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    private fun setTextColor(textView: TextView, color: Int) {
        textView.setTextColor(color)
    }

    private fun setTextColor(title: TextView, dateTime: TextView, color: Int) {
        title.setTextColor(color)
        dateTime.setTextColor(color)
    }

    private fun formatDateTimeRange(startTime: Long?, endTime: Long?): String {
        if (startTime == null && endTime == null) return "未设置时间"
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val startStr = if (startTime != null) sdf.format(Date(startTime)) else "..."
        val endStr = if (endTime != null) sdf.format(Date(endTime)) else "..."
        return "$startStr -> $endStr"
    }

    override fun getItemCount(): Int = tasks.size
}
