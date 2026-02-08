package com.hwj.cook.models

class Switch {

    private var state : Boolean =false
    fun switch(on: Boolean){
        state=on
    }

    fun isOn():Boolean{return state}
}


