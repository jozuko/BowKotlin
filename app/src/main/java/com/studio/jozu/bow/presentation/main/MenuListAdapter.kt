package com.studio.jozu.bow.presentation.main

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.doOnLayout
import com.studio.jozu.bow.R
import com.studio.jozu.bow.domain.MenuItem

class MenuListAdapter(
    private val context: Context,
    private val menuList: List<MenuItem>,
    private val onLayoutHeight: (height: Float) -> Unit,
    private val onClickItem: (position: Int) -> Unit,
) : BaseAdapter() {
    private val layoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return menuList.count()
    }

    override fun getItem(position: Int): Any {
        return menuList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val menuItem = menuList[position]
        val view = layoutInflater.inflate(R.layout.menu_button, parent, false)
        val imageView = view.findViewById<ImageView>(R.id.menu_image)
        val titleView = view.findViewById<AppCompatTextView>(R.id.menu_title)

        imageView.setImageResource(menuItem.imageRes)
        titleView.text = menuItem.title

        if (position == 0) {
            view.doOnLayout { onLayoutHeight.invoke(it.height.toFloat()) }
        }
        view.setOnClickListener { onClickItem.invoke(position) }

        return view
    }
}