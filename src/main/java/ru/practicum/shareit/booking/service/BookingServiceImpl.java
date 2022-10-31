package ru.practicum.shareit.booking.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.dto.BookingCreateRequestDto;
import ru.practicum.shareit.booking.dto.BookingCreateResponseDto;
import ru.practicum.shareit.booking.dto.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingFilter;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.repository.UserRepository;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class BookingServiceImpl implements BookingService {

  private final BookingRepository bookingRepository;
  private final UserRepository userRepository;
  private final ItemRepository itemRepository;

  @Override
  public BookingCreateResponseDto bookItem(long bookerId, BookingCreateRequestDto requestDto) {
    userRepository.findById(bookerId)
        .orElseThrow(() -> new NoSuchElementException("User with id: " + bookerId + " doesn't exists"));
    var item = itemRepository.findById(requestDto.getItemId())
        .orElseThrow(() -> new NoSuchElementException("Item with id: " + requestDto.getItemId() + " doesn't exists"));
    if (!item.getIsAvailable()) {
      throw new IllegalStateException("Item with id: " + item.getId() + " is not available");
    }

    var booking = bookingRepository.save(BookingMapper.toBooking(bookerId, requestDto));
    return BookingMapper.toBookingCreateResponseDto(booking);
  }

  @Override
  public BookingCreateResponseDto decidingOnRequest(long userId, long bookingId, boolean isApproved) {
    var booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new NoSuchElementException("Booking with id: " + bookingId + " doesn't exists"));

    if (userId != booking.getItem().getOwnerId()) {
      throw new NoSuchElementException("User with id " + userId + " not item owner");
    }

    booking.setStatus(isApproved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
    booking = bookingRepository.save(booking);
    return BookingMapper.toBookingCreateResponseDto(booking);
  }

  @Override
  public BookingCreateResponseDto getBookingInfo(long userId, long bookingId) {
    var booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new NoSuchElementException("Booking with id: " + bookingId + " doesn't exists"));

    if (userId != booking.getItem().getOwnerId() && userId != booking.getBooker().getId()) {
      throw new NoSuchElementException("User with id " + userId + " has no permits");
    }

    return BookingMapper.toBookingCreateResponseDto(booking);
  }

  @Override
  public List<BookingCreateResponseDto> getAllBookingInfo(long userId, BookingFilter state) {
    userRepository.findById(userId).orElseThrow(NoSuchElementException::new);
    List<Booking> bookings = new ArrayList<>();

    switch (state) {
      case ALL:
        bookings = bookingRepository.findAllByBookerIdOrderByStartDateTimeDesc(userId);
        break;
      case CURRENT:
        bookings = bookingRepository.findAllCurrentBookingsByBookerId(userId, LocalDateTime.now());
        break;
      case PAST:
        bookings = bookingRepository.findAllByBookerIdAndStartDateTimeBeforeOrderByStartDateTimeDesc(userId,
            LocalDateTime.now());
        break;
      case FUTURE:
        bookings = bookingRepository.findAllByBookerIdAndStartDateTimeAfterOrderByStartDateTimeDesc(userId,
            LocalDateTime.now());
        break;
      case WAITING:
        bookings = bookingRepository.findAllByBookerIdAndStatusOrderByStartDateTimeDesc(userId,
            BookingStatus.WAITING);
        break;
      case REJECTED:
        bookings = bookingRepository.findAllByBookerIdAndStatusOrderByStartDateTimeDesc(userId,
            BookingStatus.REJECTED);
        break;
    }

    return bookings.stream()
        .map(BookingMapper::toBookingCreateResponseDto)
        .collect(Collectors.toList());
  }
}
