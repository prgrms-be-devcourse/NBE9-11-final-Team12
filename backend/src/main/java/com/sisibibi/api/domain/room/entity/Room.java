package com.sisibibi.api.domain.room.entity;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    private Room(RoomStatus status) {
        this.status = status;
    }

    public static Room open() {
        return new Room(RoomStatus.OPEN);
    }

    public static Room closed() {
        return new Room(RoomStatus.CLOSED);
    }

    public void validateOpen() {
        if (status != RoomStatus.OPEN) {
            throw new CustomException(ErrorCode.ROOM_NOT_OPEN);
        }
    }
}
