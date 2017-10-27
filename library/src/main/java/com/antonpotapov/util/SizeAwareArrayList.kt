package com.antonpotapov.util

/**
 * ======================================
 * Created by awesome potapov on 23.10.17.
 * ======================================
 */
class SizeAwareArrayList<T>(private val maxSize: Int) : ArrayList<T>(maxSize) {

    public override fun removeRange(fromIndex: Int, toIndex: Int) {
        super.removeRange(fromIndex, toIndex)
    }

    override fun add(element: T): Boolean {
        val result = super.add(element)
        validateSize()
        return result
    }

    override fun add(index: Int, element: T) {
        super.add(index, element)
        validateSize()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        throw UnsupportedOperationException("adding collection is unsupported")
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        throw UnsupportedOperationException("adding collection is unsupported")
    }

    private fun validateSize() {
        if (size == maxSize) {
            removeRange(maxSize / 2, maxSize)
        }
    }
}