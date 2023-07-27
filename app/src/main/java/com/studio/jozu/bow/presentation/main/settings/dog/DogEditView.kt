package com.studio.jozu.bow.presentation.main.settings.dog

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PointF
import android.net.Uri
import android.util.AttributeSet
import android.util.SizeF
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.getSystemService
import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.studio.jozu.bow.R
import com.studio.jozu.bow.databinding.SettingDogEditViewBinding
import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.DogColor
import com.studio.jozu.bow.domain.DogGender
import com.studio.jozu.bow.domain.extension.CalendarEx.format
import java.util.*
import kotlin.math.roundToInt

class DogEditView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding = SettingDogEditViewBinding.inflate(LayoutInflater.from(context), this, true)
    private lateinit var colorViewMap: Map<DogColor, DogColorSelectionView>
    private val baseViewPoint: PointF by lazy { PointF(binding.dogEditBase.x, binding.dogEditBase.y) }
    private val thisViewSize: SizeF by lazy { SizeF(width.toFloat(), height.toFloat()) }
    private var completion: ((canceled: Boolean, dogPhotoDegree: Float) -> Unit)? = null
    private var pivotPointF: PointF? = null
    private var dogPhotoDegree = 0f
    var onClickPhoto: (() -> Unit)? = null
    var onClickBirthday: (() -> Unit)? = null

    var dog: Dog = Dog.emptyDog()
        set(value) {
            field = value.copy()
            refreshData()
        }

    private val currentColor: DogColor
        get() {
            for ((color, view) in colorViewMap) {
                if (view.isColorSelected) {
                    return color
                }
            }
            return DogColor.RED
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        colorViewMap = mapOf(
            DogColor.PURPLE to binding.dogEditColorPurple,
            DogColor.INDIGO to binding.dogEditColorIndigo,
            DogColor.BLUE to binding.dogEditColorBlue,
            DogColor.GREEN to binding.dogEditColorGreen,
            DogColor.YELLOW to binding.dogEditColorYellow,
            DogColor.ORANGE to binding.dogEditColorOrange,
            DogColor.RED to binding.dogEditColorRed,
        )

        colorViewMap.forEach { (color, view) ->
            view.onSelected = { onSelectedColor(color) }
        }

        doOnLayout {
            val baseWidth = binding.dogEditBase.width.toFloat() - (context.resources.getDimension(R.dimen.view_margin) * 2)
            val colorViewWidth = (baseWidth / 4).roundToInt()
            colorViewMap.forEach { (_, view) ->
                view.post {
                    view.updateLayoutParams<ViewGroup.LayoutParams> {
                        width = colorViewWidth
                        height = colorViewWidth
                    }
                }
            }
        }

        binding.dogEditPhoto.setOnClickListener { onClickPhoto() }
        binding.dogPhotoRotateButton.setOnClickListener { onClickPhotoRotate() }
        binding.dogEditBirthday.setOnClickListener { onClickBirthday() }
        binding.dogEditBirthday.isFocusable = false
        binding.dogEditOkButton.setOnClickListener { onClickOk() }
        binding.dogEditCancelButton.setOnClickListener { onClickCancel() }
    }

    private fun refreshData() {
        binding.dogEditName.setText(dog.name)
        binding.dogEditBirthday.setText(dog.birthday?.format("yyyy/MM/dd") ?: "")
        binding.dogEditGenderMale.isChecked = dog.gender == DogGender.MALE
        binding.dogEditGenderFemale.isChecked = dog.gender == DogGender.FEMALE

        refreshPhoto()

        onSelectedColor(dog.color)
    }

    private fun refreshPhoto() {
        binding.dogEditPhoto.setImageDrawable(null)
        dog.photo(context)
            ?.let { photo ->
                binding.dogEditPhoto.setImageURI(Uri.fromFile(photo))
                binding.dogPhotoRotateButton.isVisible = true
            }
            ?: let {
                binding.dogEditPhoto.setImageResource(R.mipmap.ic_photo_add)
                binding.dogPhotoRotateButton.isVisible = false
            }
        binding.dogEditPhoto.invalidate()
    }

    fun show(dog: Dog?, pivotPointF: PointF, completion: (canceled: Boolean, dogPhotoDegree: Float) -> Unit) {
        dog?.let {
            this.dog = it
        } ?: let {
            this.dog = Dog.emptyDog()
        }

        this.completion = completion
        this.pivotPointF = pivotPointF
        showWithAnimation()
    }

    private fun onClickOk() {
        saveCurrentInput()
        hideWithAnimation()
        completion?.invoke(false, dogPhotoDegree)
    }

    private fun onClickCancel() {
        hideWithAnimation()
        completion?.invoke(true, 0f)
    }

    private fun showWithAnimation() {
        val pivotPointF = this.pivotPointF ?: PointF(thisViewSize.width / 2, thisViewSize.height / 2)

        val scaleProperties = PropertyValuesHolder.ofFloat("scale", 0f, 1f)
        val translateXProperties = PropertyValuesHolder.ofFloat("x", pivotPointF.x, baseViewPoint.x)
        val translateYProperties = PropertyValuesHolder.ofFloat("y", pivotPointF.y, baseViewPoint.y)

        val animator = ValueAnimator.ofPropertyValuesHolder(scaleProperties, translateXProperties, translateYProperties)
        animator.duration = 300L
        animator.addUpdateListener { animation ->
            val scale = animation.getAnimatedValue("scale") as Float
            binding.dogEditBase.scaleX = scale
            binding.dogEditBase.scaleY = scale
            binding.dogEditBase.pivotX = 0f
            binding.dogEditBase.pivotY = 0f
            binding.dogEditBase.x = animation.getAnimatedValue("x") as Float
            binding.dogEditBase.y = animation.getAnimatedValue("y") as Float
        }
        animator.doOnStart {
            this.isInvisible = false
        }
        animator.start()
    }

    private fun hideWithAnimation() {
        val pivotPointF = this.pivotPointF ?: PointF(thisViewSize.width / 2, thisViewSize.height / 2)

        val scaleProperties = PropertyValuesHolder.ofFloat("scale", 1f, 0f)
        val translateXProperties = PropertyValuesHolder.ofFloat("x", baseViewPoint.x, pivotPointF.x)
        val translateYProperties = PropertyValuesHolder.ofFloat("y", baseViewPoint.y, pivotPointF.y)

        val animator = ValueAnimator.ofPropertyValuesHolder(scaleProperties, translateXProperties, translateYProperties)
        animator.duration = 300L
        animator.addUpdateListener { animation ->
            val scale = animation.getAnimatedValue("scale") as Float
            binding.dogEditBase.scaleX = scale
            binding.dogEditBase.scaleY = scale
            binding.dogEditBase.pivotX = 0f
            binding.dogEditBase.pivotY = 0f
            binding.dogEditBase.x = animation.getAnimatedValue("x") as Float
            binding.dogEditBase.y = animation.getAnimatedValue("y") as Float
        }
        animator.doOnEnd {
            this@DogEditView.isInvisible = true
            this.dogPhotoDegree = 0f
            this.binding.dogEditPhoto.rotation = 0f
        }

        animator.start()
    }

    private fun onSelectedColor(color: DogColor) {
        colorViewMap.forEach { (viewColor, view) ->
            view.isColorSelected = (color == viewColor)
        }
    }

    private fun onClickPhoto() {
        onClickPhoto?.invoke()
    }

    fun updatedPhoto() {
        dogPhotoDegree = 0f
        refreshPhoto()
    }

    private fun onClickPhotoRotate() {
        val animation = RotateAnimation(dogPhotoDegree, dogPhotoDegree + 90f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 100L
            fillAfter = false
        }

        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                binding.dogEditPhoto.rotation = dogPhotoDegree
            }

            override fun onAnimationStart(animation: Animation?) {
                dogPhotoDegree += 90f
                if (dogPhotoDegree >= 360f) {
                    dogPhotoDegree -= 360f
                }
            }
        })
        binding.dogEditPhoto.startAnimation(animation)
    }

    private fun onClickBirthday() {
        context.getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(binding.dogEditBirthday.windowToken, 0)
        onClickBirthday?.invoke()
    }

    fun updateBirthday(birthday: Calendar) {
        saveCurrentInput()
        dog = dog.copy(birthday = birthday)
    }

    private fun saveCurrentInput() {
        dog = dog.copy(
            name = binding.dogEditName.text.toString(),
            gender = if (binding.dogEditGenderMale.isChecked) DogGender.MALE else DogGender.FEMALE,
            color = currentColor
        )
    }
}