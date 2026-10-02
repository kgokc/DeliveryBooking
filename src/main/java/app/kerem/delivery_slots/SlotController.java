package app.kerem.delivery_slots;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SlotController {

    private final SlotRepository slots;

    public SlotController(SlotRepository slots) {
        this.slots = slots;
    }

    @GetMapping("/slots")
    public List<SlotResponse> list() {
        return slots.findAll().stream().map(SlotResponse::from).toList();
    }
}
