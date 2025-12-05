package com.hwj.cook.models

class SuggestCookSwitch {

    private var state : Boolean =false
    fun switch(on: Boolean){
        state=on

    }

    fun isOn():Boolean{return state}
}


