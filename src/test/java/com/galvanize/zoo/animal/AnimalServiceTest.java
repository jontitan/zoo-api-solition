package com.galvanize.zoo.animal;

import com.galvanize.zoo.habitat.HabitatDto;
import com.galvanize.zoo.habitat.HabitatEntity;
import com.galvanize.zoo.habitat.HabitatRepository;
import com.galvanize.zoo.habitat.HabitatType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock
    AnimalRepository mockAnimalRepository;

    @Mock
    HabitatRepository mockHabitatRepository;

    @InjectMocks
    AnimalService subject;

    @Test
    void create() {
        AnimalDto eagle = new AnimalDto("eagle", AnimalType.FLYING, null, null);
        subject.create(eagle);
        verify(mockAnimalRepository).save(
            new AnimalEntity("eagle", AnimalType.FLYING)
        );
    }

    @Test
    void fetchAll() {
        AnimalEntity eagle = new AnimalEntity("eagle", AnimalType.FLYING);
        eagle.setHabitat(new HabitatEntity("Eagle's Nest", HabitatType.NEST));
        when(mockAnimalRepository.findAll()).thenReturn(
            List.of(
                eagle,
                new AnimalEntity("monkey", AnimalType.WALKING)
            )
        );

        List<AnimalDto> actual = subject.fetchAll();

        assertThat(actual).isEqualTo(
            List.of(
                new AnimalDto("eagle", AnimalType.FLYING, Mood.UNHAPPY,
                    new HabitatDto("Eagle's Nest", HabitatType.NEST)),
                new AnimalDto("monkey", AnimalType.WALKING, Mood.UNHAPPY, null)
            )
        );
    }

    @Test
    void feed() {
        when(mockAnimalRepository.findByName("monkey"))
            .thenReturn(new AnimalEntity("monkey", AnimalType.WALKING));

        subject.feed("monkey");

        ArgumentCaptor<AnimalEntity> captor = ArgumentCaptor.forClass(AnimalEntity.class);
        verify(mockAnimalRepository).save(captor.capture());
        assertThat(captor.getValue().getMood()).isEqualTo(Mood.HAPPY);
    }

    @Test
    void move() throws Exception {
        when(mockAnimalRepository.findByName("monkey"))
            .thenReturn(new AnimalEntity("monkey", AnimalType.WALKING));

        HabitatEntity habitatEntity = new HabitatEntity("Monkey's Jungle", HabitatType.FOREST);
        when(mockHabitatRepository.findByName("Monkey's Jungle")).thenReturn(habitatEntity);

        subject.move("monkey", "Monkey's Jungle");

        ArgumentCaptor<AnimalEntity> captor = ArgumentCaptor.forClass(AnimalEntity.class);
        verify(mockAnimalRepository).save(captor.capture());
        assertThat(captor.getValue().getHabitat()).isSameAs(habitatEntity);
    }

    @Test
    void move_incompatible() {
        AnimalEntity monkey = new AnimalEntity("monkey", AnimalType.WALKING);
        monkey.setMood(Mood.HAPPY);
        when(mockAnimalRepository.findByName("monkey")).thenReturn(monkey);

        HabitatEntity habitatEntity = new HabitatEntity("Eagle's Nest", HabitatType.NEST);
        when(mockHabitatRepository.findByName("Eagle's Nest")).thenReturn(habitatEntity);

        assertThatThrownBy(() -> subject.move("monkey", "Eagle's Nest"))
            .isInstanceOf(IncompatibleTypeException.class);

        ArgumentCaptor<AnimalEntity> captor = ArgumentCaptor.forClass(AnimalEntity.class);
        verify(mockAnimalRepository).save(captor.capture());
        assertThat(captor.getValue().getMood()).isEqualTo(Mood.UNHAPPY);
        assertThat(captor.getValue().getHabitat()).isNull();
    }
}