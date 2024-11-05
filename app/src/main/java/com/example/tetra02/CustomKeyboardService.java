package com.example.tetra02;

import android.annotation.SuppressLint;
import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;

public class CustomKeyboardService extends InputMethodService {

    private StringBuilder currentText = new StringBuilder();
    private String currentLanguage = "EN"; // Текущий язык по умолчанию
    private boolean isShifted = false; // Переключатель для Shift (регистр букв)
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable backspaceRunnable;
    private boolean isBackspacePressed = false; // Состояние нажатия кнопки Backspace

    // Латинские буквы в верхнем и нижнем регистрах
    private static final String[] LATIN_LETTERS1 = {
            "a", "b", "c", "d", "e", "f", "g",
            "h", "i", "j", "k", "l", "m", "n",
            "o", "p", "q", "r", "s", "t", "u",
            "v", "w", "x", "y", "z"
    };

    private static final String[] LATIN_LETTERS2 = {
            "A", "B", "C", "D", "E", "F", "G",
            "H", "I", "J", "K", "L", "M", "N",
            "O", "P", "Q", "R", "S", "T", "U",
            "V", "W", "X", "Y", "Z"
    };

    // Русские буквы в верхнем и нижнем регистрах (без Ъ)
    private static final String[] RUSSIAN_LETTERS1 = {
            "а", "б", "в", "г", "д", "е", "ё",
            "ж", "з", "и", "й", "к", "л", "м",
            "н", "о", "п", "р", "с", "т", "у",
            "ф", "х", "ц", "ч", "ш", "щ", "ы",
            "ь", "э", "ю", "я"
    };

    private static final String[] RUSSIAN_LETTERS2 = {
            "А", "Б", "В", "Г", "Д", "Е", "Ё",
            "Ж", "З", "И", "Й", "К", "Л", "М",
            "Н", "О", "П", "Р", "С", "Т", "У",
            "Ф", "Х", "Ц", "Ч", "Ш", "Щ", "Ы",
            "Ь", "Э", "Ю", "Я"
    };

    private Button[] letterButtons = new Button[LATIN_LETTERS1.length]; // Массив кнопок с буквами

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateInputView() {
        // Создаем макет клавиатуры
        LinearLayout keyboardLayout = new LinearLayout(this);
        keyboardLayout.setOrientation(LinearLayout.VERTICAL);

        // Определяем количество рядов и кнопок
        int[] buttonCounts = {7, 7, 7, 10, 7, 7, 7}; // 1-3 ряды: 7 кнопок, 4 ряд: 10 кнопок, 5-7 ряды: 7 кнопок

        // Индекс для букв
        int letterIndex = 0;

        // Добавляем строки с кнопками
        for (int i = 0; i < buttonCounts.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            row.setWeightSum(buttonCounts[i]);

            // Добавляем кнопки в строку
            for (int j = 0; j < buttonCounts[i]; j++) {
                Button button = new Button(this);

                // Устанавливаем цвет фона кнопки и настраиваем функциональные кнопки
                int backgroundColor;
                boolean isFunctionKey = false;

                if (i == 0 && j == 3) { // Кнопка переключения языка
                    backgroundColor = Color.rgb(255, 182, 193); // Розовый цвет для кнопки
                    button.setText(currentLanguage.toLowerCase()); // Установим текст в нижнем регистре
                    button.setOnClickListener(v -> {
                        toggleLanguage(); // Переключаем язык
                        updateButtonLabels(); // Обновляем текст на кнопках
                    });
                    isFunctionKey = true;
                } else if (i == 1 && j == 1) {
                    backgroundColor = Color.rgb(255, 182, 193); // Розовый цвет для кнопки Shift
                    button.setText("↑");
                    button.setOnClickListener(v -> {
                        toggleShift();
                        updateButtonLabels(); // Переключаем регистр
                    });
                    isFunctionKey = true;
                } else if (i == 1 && j == 5) {
                    backgroundColor = Color.rgb(255, 182, 193); // Розовый цвет для кнопки Backspace
                    button.setText("←");
                    button.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    startBackspace();
                                    break;
                                case MotionEvent.ACTION_UP:
                                    stopBackspace();
                                    break;
                            }
                            return true;
                        }
                    });
                    isFunctionKey = true;
                } else if (i == 5 && j == 1) {
                    backgroundColor = Color.rgb(255, 182, 193); // Цвет для кнопки Space
                    button.setText(" ");
                    button.setOnClickListener(v -> {
                        // Реализация функции Space
                        InputConnection inputConnection = getCurrentInputConnection();
                        if (inputConnection != null) {
                            inputConnection.commitText(" ", 1);
                        }
                    });
                    isFunctionKey = true;
                } else if (i == 5 && j == 5) {
                    backgroundColor = Color.rgb(255, 182, 193); // Цвет для кнопки Enter
                    button.setText("↓");
                    button.setOnClickListener(v -> {
                        // Реализация функции Enter
                        InputConnection inputConnection = getCurrentInputConnection();
                        if (inputConnection != null) {
                            inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                            inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                        }
                    });
                    isFunctionKey = true;
                } else if (j == 3 && (i == 0 || i == 1 || i == 2 || i == 4 || i == 5 || i == 6)) {
                    backgroundColor = Color.rgb(255, 182, 193); // Розовый цвет для специальных кнопок
                    isFunctionKey = true;
                } else if (i == 3) {
                    backgroundColor = Color.rgb(144, 238, 144); // Светло-зеленый цвет для 4-го ряда
                    button.setText(String.valueOf(j == 9 ? 0 : j + 1));
                } else {
                    backgroundColor = Color.rgb(173, 216, 230); // Голубой цвет для остальных

                    if (currentLanguage.equals("EN") && letterIndex < LATIN_LETTERS1.length) {
                        button.setText(LATIN_LETTERS1[letterIndex]);
                        letterButtons[letterIndex++] = button; // Сохраняем кнопку в массив
                    } else if (currentLanguage.equals("RU") && letterIndex < RUSSIAN_LETTERS1.length) {
                        button.setText(RUSSIAN_LETTERS1[letterIndex]);
                        letterButtons[letterIndex++] = button; // Сохраняем кнопку в массив
                    } else {
                        button.setText("");
                    }
                }

                // Устанавливаем фон кнопки с границей
                setButtonBackground(button, backgroundColor);

                button.setTextColor(Color.BLACK);
                button.setTextSize(32);

                // Устанавливаем параметры для кнопки
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, // Ширина 0, чтобы использовать вес
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.weight = 1;
                button.setLayoutParams(params);

                // Устанавливаем отступы на кнопке
                button.setPadding(0, 0, 0, 0);

                // Добавляем обработчик нажатия на кнопку только для обычных букв
                if (!isFunctionKey) {
                    button.setOnClickListener(v -> {
                        InputConnection inputConnection = getCurrentInputConnection();
                        if (inputConnection != null) {
                            inputConnection.commitText(button.getText().toString(), 1);
                        }
                    });
                }

                row.addView(button);
            }

            keyboardLayout.addView(row);
        }

        return keyboardLayout; // Возвращаем макет клавиатуры
    }

    private void toggleShift() {
        isShifted = !isShifted; // Переключаем регистр
        updateButtonLabels(); // Обновляем текст на кнопках
    }

    private void updateButtonLabels() {
        for (int i = 0; i < letterButtons.length; i++) {
            if (letterButtons[i] != null) {
                String letter;
                if (currentLanguage.equals("EN")) {
                    letter = isShifted ? LATIN_LETTERS2[i] : LATIN_LETTERS1[i]; // Меняем регистр
                } else {
                    letter = isShifted ? RUSSIAN_LETTERS2[i] : RUSSIAN_LETTERS1[i]; // Меняем регистр
                }
                letterButtons[i].setText(letter); // Устанавливаем текст кнопки
            }
        }
    }

    private void toggleLanguage() {
        currentLanguage = currentLanguage.equals("EN") ? "RU" : "EN"; // Переключаем язык
    }

    private void startBackspace() {
        isBackspacePressed = true;
        backspaceRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBackspacePressed) {
                    InputConnection inputConnection = getCurrentInputConnection();
                    if (inputConnection != null) {
                        inputConnection.deleteSurroundingText(1, 0);
                    }
                    handler.postDelayed(this, 100); // Задержка между нажатиями
                }
            }
        };
        handler.post(backspaceRunnable);
    }

    private void stopBackspace() {
        isBackspacePressed = false; // Останавливаем нажатие
        handler.removeCallbacks(backspaceRunnable); // Убираем Runnable
    }

    private void setButtonBackground(Button button, int backgroundColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(backgroundColor); // Цвет фона
        drawable.setCornerRadius(10); // Радиус скругления
        drawable.setStroke(2, Color.BLACK); // Обводка
        button.setBackground(drawable);
    }
}
