(() => {
    const steps = Array.from(document.querySelectorAll('[data-step]'));
    const progressFill = document.getElementById('progressFill');
    const stepLabels = Array.from(document.querySelectorAll('.steps__item'));
    const backBtn = document.getElementById('backBtn');
    const nextBtn = document.getElementById('nextBtn');
    const form = document.getElementById('onboardingForm');
    const summaryList = document.getElementById('summaryList');
    const summaryTemplate = document.getElementById('summaryTemplate');
    const statusBox = document.getElementById('statusBox');

    let currentStep = 0;

    const clampNumber = (value, min, max) => {
        const normalized = typeof value === 'string' ? value.trim() : value;
        if (normalized === '' || normalized === null || normalized === undefined) return null;
        const number = Number(normalized);
        if (Number.isNaN(number)) return null;
        return Math.min(Math.max(number, min), max);
    };

    const validators = {
        0: () => {
            const name = form.childName.value.trim();
            const birthDate = form.birthDate.value;
            const guardian = form.guardian.value.trim();

            if (!name || !birthDate || !guardian) {
                showError('Completa todos los campos personales.');
                return false;
            }

            const birth = new Date(birthDate);
            const today = new Date();
            if (birth.toString() === 'Invalid Date' || birth > today) {
                showError('La fecha de nacimiento debe ser anterior a hoy.');
                return false;
            }

            return true;
        },
        1: () => {
            const weight = clampNumber(form.weight.value, 1, 200);
            const height = clampNumber(form.height.value, 30, 220);

            if (weight === null || height === null) {
                showError('Introduce un peso y altura válidos.');
                return false;
            }

            form.weight.value = weight;
            form.height.value = height;
            return true;
        },
        2: () => {
            if (!form.deviceId.value.trim() || !form.deviceAlias.value.trim()) {
                showError('Completa los datos del dispositivo.');
                return false;
            }
            return true;
        }
    };

    const showStep = (index) => {
        steps.forEach((section, idx) => {
            section.hidden = idx !== index;
        });

        stepLabels.forEach((label, idx) => {
            label.classList.toggle('active', idx === index);
        });

        const progress = ((index) / (steps.length - 1)) * 100;
        progressFill.style.width = `${progress}%`;

        backBtn.disabled = index === 0;
        nextBtn.textContent = index === steps.length - 1 ? 'Confirmar y guardar' : 'Siguiente';
        statusBox.className = 'notice';
        statusBox.textContent = '';
    };

    const showError = (message) => {
        statusBox.className = 'notice error';
        statusBox.textContent = message;
    };

    const renderSummary = () => {
        summaryList.innerHTML = '';
        const entries = [
            ['Nombre', form.childName.value],
            ['Nacimiento', form.birthDate.value],
            ['Tutor', form.guardian.value],
            ['Peso (kg)', `${form.weight.value} kg`],
            ['Altura (cm)', `${form.height.value} cm`],
            ['Tipo de sangre', form.bloodType.value || '—'],
            ['Notas médicas', form.medicalNotes.value || '—'],
            ['ID dispositivo', form.deviceId.value],
            ['Alias', form.deviceAlias.value],
        ];

        entries.forEach(([label, value]) => {
            const node = summaryTemplate.content.cloneNode(true);
            node.querySelector('dt').textContent = label;
            node.querySelector('dd').textContent = value;
            summaryList.appendChild(node);
        });
    };

    const submitData = async () => {
        const payload = {
            name: form.childName.value.trim(),
            birthDate: form.birthDate.value,
            guardian: form.guardian.value.trim(),
            health: {
                weightKg: Number(form.weight.value),
                heightCm: Number(form.height.value),
                bloodType: form.bloodType.value || null,
                notes: form.medicalNotes.value.trim() || null,
            },
            device: {
                id: form.deviceId.value.trim(),
                alias: form.deviceAlias.value.trim(),
            }
        };

        nextBtn.disabled = true;
        nextBtn.textContent = 'Guardando…';

        try {
            const response = await fetch('/api/savekid/children', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                throw new Error(`Error ${response.status}`);
            }

            statusBox.className = 'notice success';
            statusBox.textContent = 'Registro completado con éxito. ¡Dispositivo vinculado!';
            form.querySelectorAll('input, textarea, select, button').forEach(el => el.disabled = true);
            return true;
        } catch (error) {
            statusBox.className = 'notice error';
            statusBox.textContent = 'No pudimos guardar los datos. Verifica tu conexión o inténtalo de nuevo.';
            return false;
        } finally {
            nextBtn.disabled = false;
            nextBtn.textContent = 'Confirmar y guardar';
        }
    };

    backBtn.addEventListener('click', () => {
        if (currentStep > 0) {
            currentStep -= 1;
            showStep(currentStep);
        }
    });

    nextBtn.addEventListener('click', async () => {
        const validator = validators[currentStep];
        if (validator && !validator()) return;

        if (currentStep === steps.length - 1) {
            await submitData();
            return;
        }

        if (currentStep === steps.length - 2) {
            renderSummary();
        }

        currentStep += 1;
        showStep(currentStep);
    });

    // Ensure mobile keyboards do not zoom on focus
    document.querySelectorAll('input, select, textarea').forEach(input => {
        input.setAttribute('inputmode', input.type === 'number' ? 'decimal' : 'text');
    });

    // Initialize
    showStep(currentStep);
})();
