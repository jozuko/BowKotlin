package com.studio.jozu.bow.presentation.main

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.squareup.picasso.Picasso
import com.studio.jozu.bow.R
import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.extension.ContextEx.density
import com.studio.jozu.bow.presentation.picasso.PicassoRoundedSquareTransform
import kotlin.math.roundToInt

class DogSelectionDogListAdapter(
    private val context: Context,
    private val dogList: List<Dog>,
    private val selectedDog: Dog?,
) : BaseAdapter() {
    private val layoutInflater = LayoutInflater.from(context)
    private val iconRadius: Float = 8 * context.density
    private val rowInterval: Int = (16 * context.density).roundToInt()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createDogView(dogList[position], position == 0)
    }

    override fun getCount(): Int {
        return dogList.count()
    }

    override fun getItem(position: Int): Dog {
        return dogList[position]
    }

    override fun getItemId(position: Int): Long {
        return dogList[position].dogId.hashCode().toLong()
    }

    @SuppressLint("InflateParams")
    private fun createDogView(dog: Dog, isFirst: Boolean): View {
        val viewBase = layoutInflater.inflate(R.layout.calendar_dog_selection_dog_row, null, false)
        if (!isFirst) {
            viewBase.setPadding(0, rowInterval, 0, 0)
        }

        val viewSelectionBase: FrameLayout = viewBase.findViewById(R.id.dog_list_row_selected_base)
        val viewSelected: ImageView = viewBase.findViewById(R.id.dog_list_row_selected)
        val viewUnselected: ImageView = viewBase.findViewById(R.id.dog_list_row_unselected)

        if (selectedDog == null) {
            viewSelectionBase.isVisible = false
        } else {
            viewSelectionBase.isVisible = true

            if (dog.dogId == selectedDog.dogId) {
                viewSelected.isVisible = true
                viewUnselected.isVisible = false
            } else {
                viewSelected.isVisible = false
                viewUnselected.isVisible = true
            }
        }

        // 写真
        val viewPhoto: ImageView = viewBase.findViewById(R.id.viewPhoto)
        dog.photo(context)?.let { photo ->
            Picasso.get()
                .load(photo)
                .transform(PicassoRoundedSquareTransform(viewPhoto.layoutParams.width, iconRadius))
                .into(viewPhoto)
        }

        // 名前
        val viewName: TextView = viewBase.findViewById(R.id.dog_list_row_name)
        viewName.text = dog.name

        // 色
        val dogBaseColor = ContextCompat.getColor(context, dog.color.colorRes)
        val viewColor: View = viewBase.findViewById(R.id.dog_list_row_color)
        viewColor.setBackgroundColor(dogBaseColor)

        return viewBase
    }
}