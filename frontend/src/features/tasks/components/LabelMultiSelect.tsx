import {
  Checkbox,
  Chip,
  FormControl,
  FormHelperText,
  InputLabel,
  ListItemText,
  MenuItem,
  OutlinedInput,
  Select,
  type SelectChangeEvent,
} from '@mui/material';
import type { Label } from '../api/tasksApi';

interface Props {
  labels: Label[];
  value: string[];
  onChange: (labelIds: string[]) => void;
  label?: string;
  testId?: string;
}

export function LabelMultiSelect({
  labels,
  value,
  onChange,
  label = 'Labels',
  testId = 'label-multi-select',
}: Props) {
  function handleChange(event: SelectChangeEvent<string[]>) {
    const next = event.target.value;
    onChange(typeof next === 'string' ? next.split(',') : next);
  }

  return (
    <FormControl fullWidth>
      <InputLabel id={`${testId}-label`}>{label}</InputLabel>
      <Select
        labelId={`${testId}-label`}
        multiple
        value={value}
        onChange={handleChange}
        input={<OutlinedInput label={label} />}
        data-testid={testId}
        disabled={labels.length === 0}
        renderValue={(selected) => (
          <>
            {selected.map((id) => {
              const item = labels.find((l) => l.id === id);
              if (!item) return null;
              return (
                <Chip
                  key={id}
                  size="small"
                  label={item.name}
                  sx={{ mr: 0.5, bgcolor: item.color, color: '#fff' }}
                />
              );
            })}
          </>
        )}
      >
        {labels.map((item) => (
          <MenuItem key={item.id} value={item.id} data-testid={`label-option-${item.id}`}>
            <Checkbox checked={value.includes(item.id)} />
            <ListItemText primary={item.name} />
            <Chip size="small" sx={{ bgcolor: item.color, width: 16, height: 16 }} />
          </MenuItem>
        ))}
      </Select>
      {labels.length === 0 && (
        <FormHelperText data-testid={`${testId}-empty`}>
          No labels yet — create one from the Labels button
        </FormHelperText>
      )}
    </FormControl>
  );
}
