package com.aifordev.service

class BookingAlreadyCancelledException(
    message: String,
) : RuntimeException(message)
